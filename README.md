# MercadoVirtual - Punto 4: Seguridad en el Almacenamiento y Cifrado

## Objetivo

Comparar el nivel de protección entre el almacenamiento interno privado cifrado y el almacenamiento externo (SD) sin cifrar, mediante la inspección directa de los archivos con herramientas ADB.

---

## Mecanismos implementados

**1. Almacenamiento Interno Cifrado**
- Tecnología: `EncryptedSharedPreferences` con `MasterKey` respaldada por el Android KeyStore.
- Cifrado de llaves: `AES256_SIV`
- Cifrado de valores: `AES256_GCM`
- Ruta: `/data/user/0/com.example.tienda_virtual/shared_prefs/encrypted_notes_prefs.xml`

**2. Almacenamiento Externo sin Cifrar**
- Tecnología: Archivo de texto plano `.txt` en almacenamiento compartido.
- Ruta: `/sdcard/Android/data/com.example.tienda_virtual/files/Documents/nota_insegura_externa.txt`

---

## Inspección con ADB

**Intento de lectura directa del archivo interno:**
```bash
adb shell "cat /data/data/com.example.tienda_virtual/shared_prefs/encrypted_notes_prefs.xml"
```
Resultado:
```
Permission denied
```
El aislamiento de procesos de Android impide cualquier acceso externo.

---

**Lectura del archivo externo:**
```bash
adb shell "cat /sdcard/Android/data/com.example.tienda_virtual/files/Documents/nota_insegura_externa.txt"
```
Resultado:
```
DOCUMENTO CONFIDENCIAL - ALMACENAMIENTO EXTERNO
Fecha de Guardado: 2026-10-01 01:12:47
Titulo: Credenciales Vendedor y PIN Bancario
Contenido Sensible:
Usuario: admin_tienda
Clave Secreta: Udenar2026MOVIL
PIN de Transacciones: 8492
Token API: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```
Los datos son **completamente legibles en texto plano**.

---

**Extraccion forzada del archivo cifrado (via `run-as`):**
```bash
adb shell "run-as com.example.tienda_virtual cat shared_prefs/encrypted_notes_prefs.xml"
```
Resultado (fragmento):
```xml
<map>
  <string name="ASrn9IcG+y1N/kD710urebJQgkY5...">AV6nK985dxR4IpI8izodatuQXQB4...</string>
  <string name="__androidx_security_crypto_encrypted_prefs_key_keyset__">12a9011a1aab933dbd1127d11c...</string>
</map>
```
Los nombres de las claves y sus valores estan cifrados. Sin acceso al Android KeyStore del dispositivo, los datos son ilegibles.

---

## Resultados comparativos

**Almacenamiento Externo (SD) - Archivo texto plano:**
- Lectura directa por ADB: 100% legible sin restricciones.
- Lectura via run-as: 100% legible.
- Proteccion ante extravio del dispositivo: ninguna.

**Almacenamiento Interno Cifrado - EncryptedSharedPreferences (AES256):**
- Lectura directa por ADB: Permission denied.
- Lectura via run-as: Ciphertext ilegible (nombres de claves y valores cifrados).
- Proteccion ante extravio: llave custodiada en el Android KeyStore del hardware.

## Conclusion

El almacenamiento externo no ofrece ninguna proteccion criptografica. Cualquier proceso con acceso al almacenamiento compartido puede extraer la informacion confidencial. El almacenamiento interno con `EncryptedSharedPreferences` protege los datos en reposo mediante cifrado AES-256 respaldado por el Android KeyStore del hardware del dispositivo, haciendo la informacion ilegible incluso ante acceso fisico o extraccion directa de archivos.
