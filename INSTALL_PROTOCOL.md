# Instalación del Cliente Biométrico GP360

## Problema Identificado

El sistema biométrico no puede recibir los parámetros de autenticación (token) cuando se lanza desde el navegador. Esto causa el error "Unauthenticated" al intentar guardar las huellas dactilares.

## Solución

Se han creado scripts mejorados que manejan correctamente los parámetros de la URL, incluyendo el token de autenticación.

## Pasos de Instalación

### 1. Ejecutar como Administrador

**IMPORTANTE**: Debe ejecutar el siguiente comando como Administrador.

1. Abra una ventana de Command Prompt (CMD) como Administrador:
   - Presione `Windows + X`
   - Seleccione "Windows Terminal (Admin)" o "Command Prompt (Admin)"

2. Navegue a la carpeta del BiometricService:
   ```cmd
   cd C:\Users\abdia\code\GP360\BiometricService
   ```

3. Ejecute el script de corrección:
   ```cmd
   fix-protocol-wrapper.bat
   ```

4. Verá un mensaje confirmando que el protocolo fue registrado correctamente.

### 2. Verificar la Instalación

Para verificar que todo funciona correctamente:

1. En la misma ventana CMD (ya no necesita ser admin), ejecute:
   ```cmd
   test-ps-handler.bat
   ```

2. Debería ver:
   - La aplicación biométrica abriéndose
   - Los parámetros parseados correctamente, incluyendo el token

### 3. Probar desde el Navegador

1. Abra el sistema GP360 en el navegador
2. Vaya a la sección de captura biométrica
3. Haga clic en el botón de captura
4. El navegador le preguntará si desea abrir la aplicación - confirme
5. La aplicación biométrica debería abrirse con todos los parámetros

## Archivos Importantes

- `protocol-wrapper.bat` - Maneja la recepción de la URL desde el registro
- `protocol-handler.ps1` - Script PowerShell que parsea los parámetros y ejecuta el JAR
- `fix-protocol-wrapper.bat` - Registra el protocolo en Windows (requiere admin)

## Solución de Problemas

### Si sigue sin funcionar:

1. **Verifique el token en el frontend**:
   - Abra las herramientas de desarrollo del navegador (F12)
   - Vaya a la pestaña Console
   - Cuando haga clic en el botón biométrico, debería ver la URL completa con todos los parámetros

2. **Prueba manual**:
   - Abra CMD y ejecute:
   ```cmd
   start gp360://enroll?id=23^&type=inmate^&token=SU_TOKEN_AQUI^&api=http://127.0.0.1:8000/api
   ```
   - Reemplace `SU_TOKEN_AQUI` con un token válido del sistema

3. **Revise los logs**:
   - El script mostrará todos los parámetros recibidos
   - Si el token aparece como "[NOT PROVIDED]", el problema está en el envío desde el navegador

## Nota sobre el Token

El token de autenticación es necesario para que el JAR pueda comunicarse con el API backend. Sin el token, las huellas se capturan pero no se pueden guardar en la base de datos.