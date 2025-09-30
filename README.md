# GP360 BiometricService

Servicio de captura y verificación biométrica para el sistema GP360 usando DigitalPersona U.are.U SDK.

## Requisitos

- Java 11+
- Windows OS (requerido para drivers DigitalPersona)
- DigitalPersona U.are.U 4500/5000 fingerprint reader
- MySQL 8.0+
- U.are.U SDK 3.x

## Instalación

1. Instalar drivers DigitalPersona U.are.U
2. Configurar el entorno:
   ```bash
   setup.bat
   ```

## Compilación

```bash
build.bat
```

Genera dos archivos JAR en `dist/`:
- `BiometricService.jar` - Aplicación de enrollment
- `BiometricVerification.jar` - Aplicación de verificación

## Uso

### Enrollment (Registro de huellas)
```bash
run.bat [inmateId]
```
- Si no se proporciona inmateId, solicitará ingresarlo
- Captura las 10 huellas digitales
- Guarda directamente en base de datos MySQL

### Verificación
```bash
verificar.bat
```
- Verifica huellas contra la base de datos
- Muestra coincidencias encontradas

### Utilidades
```bash
kill-java.bat  # Termina procesos Java colgados
```

## Configuración Base de Datos

La aplicación se conecta a MySQL con estos parámetros por defecto:
- Host: localhost:3306
- Database: gp360_dev
- Usuario: root
- Password: (vacío)

Para cambiar la configuración, modificar en el código fuente:
- `DatabaseConfig.java`
- `BiometricApplication.java`

## Estructura de Tablas

### inmate_biometric_data
```sql
CREATE TABLE inmate_biometric_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inmate_id BIGINT,
    fingerprint_1 through fingerprint_10 BLOB,
    quality_1 through quality_10 INT,
    capture_date TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

## Integración con Laravel

El servicio se comunica con el backend Laravel en `http://localhost:8000` para:
- Verificar existencia de registros
- Actualizar estados de captura
- Registrar logs de actividad

## Troubleshooting

### Error: "No se puede encontrar el lector"
- Verificar que el lector esté conectado
- Reinstalar drivers DigitalPersona
- Ejecutar `setup.bat` nuevamente

### Error: "Cannot connect to database"
- Verificar que MySQL esté ejecutándose
- Verificar credenciales en el código
- Verificar que la base de datos `gp360_dev` exista

### Error de compilación
- Verificar que Java 11+ esté instalado
- Verificar que todas las librerías estén en `lib/`
- Ejecutar `setup.bat` antes de `build.bat`

## Archivos principales

- `build.bat` - Script de compilación
- `setup.bat` - Configuración inicial
- `run.bat` - Ejecutar enrollment
- `verificar.bat` - Ejecutar verificación
- `kill-java.bat` - Terminar procesos Java