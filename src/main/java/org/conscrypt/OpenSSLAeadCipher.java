package org.conscrypt;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import org.conscrypt.OpenSSLCipher;

public abstract class OpenSSLAeadCipher extends OpenSSLCipher {
    static final int DEFAULT_TAG_SIZE_BITS = 128;
    private static int lastGlobalMessageSize = 32;
    private byte[] aad;
    byte[] buf;
    int bufCount;
    long evpAead;
    private boolean mustInitialize;
    private byte[] previousIv;
    private byte[] previousKey;
    int tagLengthInBytes;

    /* access modifiers changed from: package-private */
    public boolean allowsNonceReuse() {
        return false;
    }

    /* access modifiers changed from: package-private */
    public abstract long getEVP_AEAD(int i) throws InvalidKeyException;

    /* access modifiers changed from: package-private */
    public int getOutputSizeForUpdate(int i) {
        return 0;
    }

    public OpenSSLAeadCipher(OpenSSLCipher.Mode mode) {
        super(mode, OpenSSLCipher.Padding.NOPADDING);
    }

    private void checkInitialization() {
        if (this.mustInitialize) {
            throw new IllegalStateException("Cannot re-use same key and IV for multiple encryptions");
        }
    }

    private boolean arraysAreEqual(byte[] bArr, byte[] bArr2) {
        if (bArr.length != bArr2.length) {
            return false;
        }
        byte b = 0;
        for (int i = 0; i < bArr.length; i++) {
            b |= bArr[i] ^ bArr2[i];
        }
        if (b == 0) {
            return true;
        }
        return false;
    }

    private void expand(int i) {
        int i2 = this.bufCount;
        int i3 = i2 + i;
        byte[] bArr = this.buf;
        if (i3 > bArr.length) {
            byte[] bArr2 = new byte[((i + i2) * 2)];
            System.arraycopy(bArr, 0, bArr2, 0, i2);
            this.buf = bArr2;
        }
    }

    private void reset() {
        this.aad = null;
        int i = lastGlobalMessageSize;
        byte[] bArr = this.buf;
        if (bArr == null) {
            this.buf = new byte[i];
        } else {
            int i2 = this.bufCount;
            if (i2 > 0 && i2 != i) {
                lastGlobalMessageSize = i2;
                if (bArr.length != i2) {
                    this.buf = new byte[i2];
                }
            }
        }
        this.bufCount = 0;
    }

    /* access modifiers changed from: package-private */
    public void engineInitInternal(byte[] bArr, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        byte[] bArr2 = null;
        int i = 128;
        if (algorithmParameterSpec != null) {
            GCMParameters fromGCMParameterSpec = Platform.fromGCMParameterSpec(algorithmParameterSpec);
            if (fromGCMParameterSpec != null) {
                bArr2 = fromGCMParameterSpec.getIV();
                i = fromGCMParameterSpec.getTLen();
            } else if (algorithmParameterSpec instanceof IvParameterSpec) {
                bArr2 = ((IvParameterSpec) algorithmParameterSpec).getIV();
            }
        }
        checkSupportedTagLength(i);
        this.tagLengthInBytes = i / 8;
        boolean isEncrypting = isEncrypting();
        long evp_aead = getEVP_AEAD(bArr.length);
        this.evpAead = evp_aead;
        int EVP_AEAD_nonce_length = NativeCrypto.EVP_AEAD_nonce_length(evp_aead);
        if (bArr2 != null || EVP_AEAD_nonce_length == 0) {
            if (EVP_AEAD_nonce_length == 0 && bArr2 != null) {
                throw new InvalidAlgorithmParameterException("IV not used in " + this.mode + " mode");
            } else if (!(bArr2 == null || bArr2.length == EVP_AEAD_nonce_length)) {
                throw new InvalidAlgorithmParameterException("Expected IV length of " + EVP_AEAD_nonce_length + " but was " + bArr2.length);
            }
        } else if (isEncrypting) {
            bArr2 = new byte[EVP_AEAD_nonce_length];
            if (secureRandom != null) {
                secureRandom.nextBytes(bArr2);
            } else {
                NativeCrypto.RAND_bytes(bArr2);
            }
        } else {
            throw new InvalidAlgorithmParameterException("IV must be specified in " + this.mode + " mode");
        }
        if (isEncrypting() && bArr2 != null && !allowsNonceReuse()) {
            byte[] bArr3 = this.previousKey;
            if (bArr3 == null || this.previousIv == null || !arraysAreEqual(bArr3, bArr) || !arraysAreEqual(this.previousIv, bArr2)) {
                this.previousKey = bArr;
                this.previousIv = bArr2;
            } else {
                this.mustInitialize = true;
                throw new InvalidAlgorithmParameterException("When using AEAD key and IV must not be re-used");
            }
        }
        this.mustInitialize = false;
        this.iv = bArr2;
        reset();
    }

    /* access modifiers changed from: package-private */
    public void checkSupportedTagLength(int i) throws InvalidAlgorithmParameterException {
        if (i % 8 != 0) {
            throw new InvalidAlgorithmParameterException("Tag length must be a multiple of 8; was " + i);
        }
    }

    /* access modifiers changed from: protected */
    public int engineDoFinal(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        if (bArr2 == null || getOutputSizeForFinal(i2) <= bArr2.length - i3) {
            return super.engineDoFinal(bArr, i, i2, bArr2, i3);
        }
        throw new ShortBufferException("Insufficient output space");
    }

    /* access modifiers changed from: package-private */
    public int updateInternal(byte[] bArr, int i, int i2, byte[] bArr2, int i3, int i4) throws ShortBufferException {
        checkInitialization();
        if (this.buf != null) {
            ArrayUtils.checkOffsetAndCount(bArr.length, i, i2);
            if (i2 <= 0) {
                return 0;
            }
            expand(i2);
            System.arraycopy(bArr, i, this.buf, this.bufCount, i2);
            this.bufCount += i2;
            return 0;
        }
        throw new IllegalStateException("Cipher not initialized");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0023, code lost:
        r6 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0033, code lost:
        throw ((javax.crypto.BadPaddingException) new javax.crypto.BadPaddingException().initCause(r6.getTargetException()));
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Removed duplicated region for block: B:10:0x0023 A[ExcHandler: InvocationTargetException (r6v3 'e' java.lang.reflect.InvocationTargetException A[CUSTOM_DECLARE]), Splitter:B:4:0x0013] */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x0037 A[RETURN] */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0038  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void throwAEADBadTagExceptionIfAvailable(java.lang.String r6, java.lang.Throwable r7) throws javax.crypto.BadPaddingException {
        /*
            r5 = this;
            java.lang.String r0 = "javax.crypto.AEADBadTagException"
            java.lang.Class r0 = java.lang.Class.forName(r0)     // Catch:{ Exception -> 0x0039 }
            r1 = 1
            java.lang.Class[] r2 = new java.lang.Class[r1]     // Catch:{ Exception -> 0x0039 }
            java.lang.Class<java.lang.String> r3 = java.lang.String.class
            r4 = 0
            r2[r4] = r3     // Catch:{ Exception -> 0x0039 }
            java.lang.reflect.Constructor r0 = r0.getConstructor(r2)     // Catch:{ Exception -> 0x0039 }
            r2 = 0
            java.lang.Object[] r1 = new java.lang.Object[r1]     // Catch:{ IllegalAccessException | InstantiationException -> 0x0034, InvocationTargetException -> 0x0023 }
            r1[r4] = r6     // Catch:{ IllegalAccessException | InstantiationException -> 0x0034, InvocationTargetException -> 0x0023 }
            java.lang.Object r6 = r0.newInstance(r1)     // Catch:{ IllegalAccessException | InstantiationException -> 0x0034, InvocationTargetException -> 0x0023 }
            javax.crypto.BadPaddingException r6 = (javax.crypto.BadPaddingException) r6     // Catch:{ IllegalAccessException | InstantiationException -> 0x0034, InvocationTargetException -> 0x0023 }
            r6.initCause(r7)     // Catch:{ IllegalAccessException | InstantiationException -> 0x0021, InvocationTargetException -> 0x0023 }
            goto L_0x0035
        L_0x0021:
            r2 = r6
            goto L_0x0034
        L_0x0023:
            r6 = move-exception
            javax.crypto.BadPaddingException r7 = new javax.crypto.BadPaddingException
            r7.<init>()
            java.lang.Throwable r6 = r6.getTargetException()
            java.lang.Throwable r6 = r7.initCause(r6)
            javax.crypto.BadPaddingException r6 = (javax.crypto.BadPaddingException) r6
            throw r6
        L_0x0034:
            r6 = r2
        L_0x0035:
            if (r6 != 0) goto L_0x0038
            return
        L_0x0038:
            throw r6
        L_0x0039:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: org.conscrypt.OpenSSLAeadCipher.throwAEADBadTagExceptionIfAvailable(java.lang.String, java.lang.Throwable):void");
    }

    /* access modifiers changed from: package-private */
    public int doFinalInternal(byte[] bArr, int i, int i2) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        int i3;
        checkInitialization();
        try {
            if (isEncrypting()) {
                i3 = NativeCrypto.EVP_AEAD_CTX_seal(this.evpAead, this.encodedKey, this.tagLengthInBytes, bArr, i, this.iv, this.buf, 0, this.bufCount, this.aad);
            } else {
                i3 = NativeCrypto.EVP_AEAD_CTX_open(this.evpAead, this.encodedKey, this.tagLengthInBytes, bArr, i, this.iv, this.buf, 0, this.bufCount, this.aad);
            }
            if (isEncrypting()) {
                this.mustInitialize = true;
            }
            reset();
            return i3;
        } catch (BadPaddingException e) {
            throwAEADBadTagExceptionIfAvailable(e.getMessage(), e.getCause());
            throw e;
        }
    }

    /* access modifiers changed from: package-private */
    public void checkSupportedPadding(OpenSSLCipher.Padding padding) throws NoSuchPaddingException {
        if (padding != OpenSSLCipher.Padding.NOPADDING) {
            throw new NoSuchPaddingException("Must be NoPadding for AEAD ciphers");
        }
    }

    /* access modifiers changed from: package-private */
    public int getOutputSizeForFinal(int i) {
        return this.bufCount + i + (isEncrypting() ? NativeCrypto.EVP_AEAD_max_overhead(this.evpAead) : 0);
    }

    /* access modifiers changed from: protected */
    public void engineUpdateAAD(byte[] bArr, int i, int i2) {
        checkInitialization();
        byte[] bArr2 = this.aad;
        if (bArr2 == null) {
            this.aad = Arrays.copyOfRange(bArr, i, i2 + i);
            return;
        }
        byte[] bArr3 = new byte[(bArr2.length + i2)];
        System.arraycopy(bArr2, 0, bArr3, 0, bArr2.length);
        System.arraycopy(bArr, i, bArr3, this.aad.length, i2);
        this.aad = bArr3;
    }

    /* access modifiers changed from: protected */
    public void engineUpdateAAD(ByteBuffer byteBuffer) {
        checkInitialization();
        byte[] bArr = this.aad;
        if (bArr == null) {
            byte[] bArr2 = new byte[byteBuffer.remaining()];
            this.aad = bArr2;
            byteBuffer.get(bArr2);
            return;
        }
        byte[] bArr3 = new byte[(bArr.length + byteBuffer.remaining())];
        byte[] bArr4 = this.aad;
        System.arraycopy(bArr4, 0, bArr3, 0, bArr4.length);
        byteBuffer.get(bArr3, this.aad.length, byteBuffer.remaining());
        this.aad = bArr3;
    }
}
