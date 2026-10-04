"""One-time first upload key; export only encrypted backup and public certificate."""
import os, pathlib, secrets, subprocess, json, base64
from cryptography.hazmat.primitives import serialization, hashes
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
p = pathlib.Path(os.environ["RUNNER_TEMP"]) / "eftermotet-signing"
p.mkdir(mode=0o700)
password = secrets.token_urlsafe(32)
os.environ["SIGNING_PASSWORD"] = password
subprocess.run(["keytool", "-genkeypair", "-keystore", str(p / "upload.p12"),
    "-storetype", "PKCS12", "-storepass:env", "SIGNING_PASSWORD", "-keypass:env", "SIGNING_PASSWORD",
    "-alias", "eftermotet-upload", "-keyalg", "RSA", "-keysize", "3072",
    "-sigalg", "SHA256withRSA", "-validity", "10000", "-dname", "CN=EfterMotet Upload"], check=True)
out = pathlib.Path("release-output")
out.mkdir(exist_ok=True)
subprocess.run(["keytool", "-exportcert", "-rfc", "-keystore", str(p / "upload.p12"),
    "-storepass:env", "SIGNING_PASSWORD", "-alias", "eftermotet-upload", "-file", str(out / "upload-certificate.pem")], check=True)
data = json.dumps({"keystore": base64.b64encode((p / "upload.p12").read_bytes()).decode(),
    "password": password, "alias": "eftermotet-upload"}).encode()
pub = serialization.load_pem_public_key(pathlib.Path("release/recovery-public.pem").read_bytes())
aes = AESGCM.generate_key(bit_length=256)
nonce = os.urandom(12)
sealed = {"version": 1, "key": base64.b64encode(pub.encrypt(aes, padding.OAEP(
    mgf=padding.MGF1(hashes.SHA256()), algorithm=hashes.SHA256(), label=None))).decode(),
    "nonce": base64.b64encode(nonce).decode(),
    "ciphertext": base64.b64encode(AESGCM(aes).encrypt(nonce, data, b"EfterMotet upload key v1")).decode()}
(out / "upload-key.encrypted.json").write_text(json.dumps(sealed))
print("::add-mask::" + password)
with open(os.environ["GITHUB_ENV"], "a") as f:
    f.write("RELEASE_STORE_FILE=" + str(p / "upload.p12") + "\n")
    f.write("RELEASE_STORE_PASSWORD=" + password + "\n")
    f.write("RELEASE_KEY_PASSWORD=" + password + "\n")
    f.write("RELEASE_KEY_ALIAS=eftermotet-upload\n")
