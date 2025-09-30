# Solución Final - Cliente Biométrico GP360

## Resumen del Problema

El cliente biométrico no recibía el token de autenticación porque Windows cortaba la URL en el primer carácter `&`, enviando solo `gp360://enroll/?id=24` sin los demás parámetros.

## Solución Implementada

Se implementó codificación Base64 para los parámetros, evitando así problemas con caracteres especiales en las URLs.

### Cambios Realizados:

1. **Frontend** (`AdmissionWizard.vue`):
   - Los parámetros ahora se codifican en Base64
   - La URL es simple: `gp360://enroll?data=[BASE64_DATA]`

2. **Protocol Handler** (`protocol-handler.ps1`):
   - Decodifica automáticamente los datos Base64
   - Extrae todos los parámetros incluyendo el token
   - Mantiene compatibilidad con el formato antiguo

## Instalación

### Paso 1: Registrar el Protocol Handler (Requiere Admin)

Abra CMD como Administrador y ejecute:
```cmd
cd C:\Users\abdia\code\GP360\BiometricService
fix-protocol-wrapper.bat
```

### Paso 2: Verificar la Instalación

Pruebe con datos de ejemplo:
```cmd
powershell.exe -ExecutionPolicy Bypass -File test-direct-base64.ps1
```

Debería ver:
- El cliente biométrico abrirse
- Todos los parámetros parseados correctamente
- El token visible (primeros 20 caracteres)

### Paso 3: Recompilar el Frontend

```bash
cd frontend
npm run build
```

## Verificación

Para verificar que todo funciona:

1. Abra el sistema GP360 en el navegador
2. Vaya a la sección de captura biométrica
3. Abra las herramientas de desarrollo (F12)
4. Busque en la consola la línea que dice: `Opening biometric client with URL:`
5. Debería ver algo como: `gp360://enroll?data=eyJhcGk...`
6. Haga clic en el botón de captura biométrica
7. El cliente debería abrirse con todos los parámetros

## Cómo Funciona

1. El frontend crea un objeto JSON con todos los parámetros:
   ```javascript
   {
     id: 24,
     type: 'inmate',
     token: '1|kbS5uIs8njq...',
     api: 'http://127.0.0.1:8000/api'
   }
   ```

2. Este JSON se convierte a Base64:
   ```
   eyJhcGkiOiJodHRwOi8vMTI3LjAuMC4xOjgwMDAvYXBpIiwiaWQiOjI0LC...
   ```

3. Se crea una URL simple:
   ```
   gp360://enroll?data=eyJhcGkiOiJodHRwOi8vMTI3LjAuMC4xOjgwMDAvYXBpIiwiaWQiOjI0LC...
   ```

4. El protocol handler decodifica el Base64 y extrae todos los parámetros

## Ventajas de esta Solución

- ✅ No hay problemas con caracteres especiales (`&`, `|`, `:`, `/`)
- ✅ El token llega completo al cliente biométrico
- ✅ Funciona con URLs largas y complejas
- ✅ Compatible con todos los navegadores
- ✅ No requiere cambios en el JAR

## Troubleshooting

### Si el token sigue sin llegar:

1. Verifique que el frontend esté usando la versión actualizada
2. Limpie el caché del navegador (Ctrl+F5)
3. Verifique en la consola del navegador que la URL tenga `?data=`

### Si el cliente no se abre:

1. Verifique que el protocolo esté registrado:
   ```cmd
   reg query HKCR\gp360
   ```

2. Reinstale el protocol handler (como admin):
   ```cmd
   fix-protocol-wrapper.bat
   ```

### Para obtener un token válido actual:

1. Abra el sistema GP360
2. Inicie sesión
3. Abra las herramientas de desarrollo (F12)
4. En la consola, escriba:
   ```javascript
   localStorage.getItem('auth_token')
   ```
5. Use ese token para pruebas manuales

## Resultado Esperado

Cuando todo funciona correctamente, verá:

```
========================================
GP360 Biometric Client
========================================

Received URL: gp360://enroll?data=eyJ...

Decoding Base64 parameters...
Parsed Parameters:
- ID: 24
- Type: App\Models\Inmate
- Token: 1|kbS5uIs8njq55xIrxw...
- API: http://127.0.0.1:8000/api

Starting BiometricService.jar...
[El cliente se abre y puede guardar las huellas sin errores]
```

## Contacto

Si encuentra problemas, verifique:
1. Que el token en localStorage sea válido
2. Que el backend esté ejecutándose en el puerto 8000
3. Que el JAR esté compilado y en `dist/BiometricService.jar`