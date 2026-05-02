package com.example.pigeon.data.identity

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.pigeon.proto.PigeonEvent
import com.example.pigeon.proto.VerificationMessage
import com.google.protobuf.ByteString
import dagger.hilt.android.qualifiers.ApplicationContext
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Security
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns this device's Ed25519 identity keypair — the cryptographic root of the
 * Distributed Trust model. The keypair is generated on first access and pinned
 * in [EncryptedSharedPreferences], where the OS-backed master key keeps the
 * private bytes off-disk in plaintext.
 *
 * The [signerId] is `SHA-256(X.509 SubjectPublicKeyInfo)` of the public key —
 * a stable 64-char hex fingerprint that replaces ANDROID_ID as the device's
 * identity on the mesh. Receivers can verify a peer's claimed [signerId] by
 * recomputing the same hash over the [publicKeyBytes] embedded in each signed
 * proto message.
 *
 * BouncyCastle is registered as the JCE provider for Ed25519 because the
 * platform's `KeyPairGenerator.getInstance("Ed25519")` only exists on API 33+,
 * and minSdk is 26. Using BC everywhere gives identical behavior across the
 * supported API range.
 */
@Singleton
class IdentityKeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    init {
        ensureBouncyCastleRegistered()
    }

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Volatile private var cached: Identity? = null

    /**
     * The device's persistent identity. Loaded from secure storage on first
     * access, generated and persisted if absent. Stable for the lifetime of
     * the install (uninstall + reinstall produces a new identity).
     */
    val identity: Identity
        get() = cached ?: synchronized(this) {
            cached ?: loadOrGenerateIdentity().also { cached = it }
        }

    /** Convenience accessor — SHA-256 hex fingerprint of the public key. */
    val signerId: String get() = identity.signerId

    /** Convenience accessor — X.509 SPKI bytes for embedding in proto messages. */
    val publicKeyBytes: ByteArray get() = identity.publicKeyBytes

    /**
     * Sign [data] with this device's private key. Caller is responsible for
     * defining the canonical byte form of whatever it wants to sign — for
     * proto messages the convention is "serialize with signature field cleared".
     */
    fun sign(data: ByteArray): ByteArray {
        val sig = Signature.getInstance(ALGORITHM, PROVIDER_NAME)
        sig.initSign(identity.keyPair.private)
        sig.update(data)
        return sig.sign()
    }

    /**
     * Verify that [signature] is a valid Ed25519 signature over [data] under
     * the public key encoded in [publicKeyBytes] (X.509 SPKI form, as carried
     * on the wire). Returns false on any error — malformed key, malformed
     * signature, mismatch — never throws.
     */
    fun verify(data: ByteArray, signature: ByteArray, publicKeyBytes: ByteArray): Boolean {
        return try {
            val pub = KeyFactory.getInstance(ALGORITHM, PROVIDER_NAME)
                .generatePublic(X509EncodedKeySpec(publicKeyBytes))
            val sig = Signature.getInstance(ALGORITHM, PROVIDER_NAME)
            sig.initVerify(pub)
            sig.update(data)
            sig.verify(signature)
        } catch (t: Throwable) {
            Log.w(TAG, "verify() failed: ${t.javaClass.simpleName}: ${t.message}")
            false
        }
    }

    /** Compute the canonical signerId hex string for an arbitrary public key. */
    fun signerIdFor(publicKeyBytes: ByteArray): String =
        sha256Hex(publicKeyBytes)

    // ── Proto-aware helpers ──────────────────────────────────────────────────
    //
    // Signing protocol for both PigeonEvent and VerificationMessage:
    //   1. Stamp the identity fields the signer controls (signerId/creatorDeviceId,
    //      public_key) onto the builder.
    //   2. Clear the signature field, serialize the message to bytes.
    //   3. Sign those bytes; copy the signature back into a final builder.
    //
    // Verification mirrors the same canonicalization: clear signature, serialize,
    // and verify against the public key carried on the wire (after first checking
    // that SHA-256(public_key) matches the claimed signerId — without that check,
    // a forger could send a valid signature under their own key while pretending
    // to be someone else).

    /**
     * Sign a [PigeonEvent.Builder] under this device's identity. Caller is
     * responsible for populating all non-identity fields *before* calling this
     * — `creatorDeviceId`, `creator_public_key`, and `signature` are
     * (re)written by the manager and any pre-set values are overwritten.
     */
    fun signEvent(builder: PigeonEvent.Builder): PigeonEvent {
        val canonical = builder
            .setCreatorDeviceId(signerId)
            .setCreatorPublicKey(ByteString.copyFrom(publicKeyBytes))
            .clearSignature()
            .build()
        val sig = sign(canonical.toByteArray())
        return canonical.toBuilder()
            .setSignature(ByteString.copyFrom(sig))
            .build()
    }

    /** Sign a [VerificationMessage.Builder] under this device's identity. */
    fun signVerification(builder: VerificationMessage.Builder): VerificationMessage {
        val canonical = builder
            .setSignerId(signerId)
            .setSignerPublicKey(ByteString.copyFrom(publicKeyBytes))
            .clearSignature()
            .build()
        val sig = sign(canonical.toByteArray())
        return canonical.toBuilder()
            .setSignature(ByteString.copyFrom(sig))
            .build()
    }

    /**
     * Validate a received [PigeonEvent]. Returns true iff:
     *   - signature and creator_public_key are both present
     *   - SHA-256(creator_public_key) equals creatorDeviceId
     *   - the signature verifies under creator_public_key over the message
     *     bytes with the signature field cleared
     */
    fun verifyPigeonEvent(proto: PigeonEvent): Boolean {
        if (proto.signature.isEmpty || proto.creatorPublicKey.isEmpty) return false
        val pkBytes = proto.creatorPublicKey.toByteArray()
        if (signerIdFor(pkBytes) != proto.creatorDeviceId) return false
        val canonicalBytes = proto.toBuilder().clearSignature().build().toByteArray()
        return verify(canonicalBytes, proto.signature.toByteArray(), pkBytes)
    }

    /** Mirror of [verifyPigeonEvent] for [VerificationMessage]. */
    fun verifyVerificationMessage(proto: VerificationMessage): Boolean {
        if (proto.signature.isEmpty || proto.signerPublicKey.isEmpty) return false
        val pkBytes = proto.signerPublicKey.toByteArray()
        if (signerIdFor(pkBytes) != proto.signerId) return false
        val canonicalBytes = proto.toBuilder().clearSignature().build().toByteArray()
        return verify(canonicalBytes, proto.signature.toByteArray(), pkBytes)
    }

    private fun loadOrGenerateIdentity(): Identity {
        val privB64 = prefs.getString(KEY_PRIVATE, null)
        val pubB64 = prefs.getString(KEY_PUBLIC, null)

        if (privB64 != null && pubB64 != null) {
            return runCatching {
                val privBytes = Base64.decode(privB64, Base64.NO_WRAP)
                val pubBytes = Base64.decode(pubB64, Base64.NO_WRAP)
                val kp = restoreKeyPair(privBytes, pubBytes)
                Log.d(TAG, "Loaded existing Ed25519 identity (signerId=${sha256Hex(pubBytes).take(12)}…)")
                Identity(kp, sha256Hex(pubBytes), pubBytes)
            }.getOrElse { t ->
                // Stored bytes are corrupt or unreadable — clear and regenerate
                // rather than crashing the app. This is a one-way migration:
                // the old signerId becomes orphaned, which is acceptable for an
                // identity that other peers haven't yet verified.
                Log.e(TAG, "Failed to restore identity, regenerating", t)
                prefs.edit().remove(KEY_PRIVATE).remove(KEY_PUBLIC).apply()
                generateAndPersist()
            }
        }

        return generateAndPersist()
    }

    private fun generateAndPersist(): Identity {
        val kp = KeyPairGenerator.getInstance(ALGORITHM, PROVIDER_NAME).generateKeyPair()
        val pubBytes = kp.public.encoded   // X.509 SubjectPublicKeyInfo
        val privBytes = kp.private.encoded // PKCS#8

        prefs.edit()
            .putString(KEY_PRIVATE, Base64.encodeToString(privBytes, Base64.NO_WRAP))
            .putString(KEY_PUBLIC, Base64.encodeToString(pubBytes, Base64.NO_WRAP))
            .apply()

        val signerId = sha256Hex(pubBytes)
        Log.d(TAG, "Generated new Ed25519 identity (signerId=${signerId.take(12)}…)")
        return Identity(kp, signerId, pubBytes)
    }

    private fun restoreKeyPair(privBytes: ByteArray, pubBytes: ByteArray): KeyPair {
        val kf = KeyFactory.getInstance(ALGORITHM, PROVIDER_NAME)
        val priv = kf.generatePrivate(PKCS8EncodedKeySpec(privBytes))
        val pub = kf.generatePublic(X509EncodedKeySpec(pubBytes))
        return KeyPair(pub, priv)
    }

    /**
     * Holds the live keypair plus pre-computed derived values. Kept in a single
     * value object so callers never see a half-initialized identity (e.g. a
     * keyPair without its matching signerId).
     */
    data class Identity(
        val keyPair: KeyPair,
        val signerId: String,
        val publicKeyBytes: ByteArray
    ) {
        // Default equals/hashCode on data classes with ByteArray would compare
        // by reference, which is wrong for value semantics. We override only
        // when needed — for now, callers compare via signerId.
        override fun equals(other: Any?): Boolean = this === other
        override fun hashCode(): Int = signerId.hashCode()
    }

    companion object {
        private const val TAG = "[IDENTITY]"
        private const val PREFS_NAME = "pigeon_identity_keystore"
        private const val KEY_PRIVATE = "ed25519_private_pkcs8_b64"
        private const val KEY_PUBLIC = "ed25519_public_x509_b64"
        private const val ALGORITHM = "Ed25519"
        private val PROVIDER_NAME: String = BouncyCastleProvider.PROVIDER_NAME

        /**
         * Android ships a stripped-down BC stub under the same provider name
         * ("BC"). Removing it before adding the full BC ensures our Ed25519
         * lookups resolve against the imported provider, not the platform stub.
         * Idempotent — safe to call multiple times.
         */
        @Synchronized
        private fun ensureBouncyCastleRegistered() {
            if (Security.getProvider(PROVIDER_NAME) !is BouncyCastleProvider) {
                Security.removeProvider(PROVIDER_NAME)
                Security.addProvider(BouncyCastleProvider())
            }
        }

        private fun sha256Hex(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            val sb = StringBuilder(digest.size * 2)
            for (b in digest) {
                val v = b.toInt() and 0xFF
                sb.append(HEX[v ushr 4])
                sb.append(HEX[v and 0x0F])
            }
            return sb.toString()
        }

        private val HEX = "0123456789abcdef".toCharArray()
    }
}
