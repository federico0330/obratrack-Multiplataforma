# Progreso de ObraTrack Multiplataforma

## Hecho y verificado
- Gradle configurado; app ejecutada en teléfono Android físico.
- Login Firebase probado con una cuenta existente; registro implementado.
- Capas separadas: `core/domain`, `core/data`, `core/firebase`, features Android y `androidApp`.
- Login/registro con ViewModels, pantallas Compose, errores tipados y dependencias desde `AppContainer`.
- Textos de auth/home en es/en/it; el italiano requiere revisión.
- Última verificación registrada antes de esta feature: 121 tests y `assembleDebug` OK.
- Kotlin 2.4.21, AGP 9.4.1, Gradle 9.6.0, JDK 21. Core compila para iOS; no hay UI ni prueba de app iOS.

## Crear y listar obras
- Dominio, puerto Firestore, repositorio, feature Android y navegación inicial están implementados en la rama `feat/obras-create-list`.
- La consulta filtra por `userId`; la creación conserva `obras/{id}`, los campos nativos y fechas Firestore Timestamp.
- Pasan los tests de domain, data, firebase y obras, y `:androidApp:assembleDebug`.
- En teléfono: sesión existente, lectura, validación, creación y persistencia tras reiniciar probadas con Firebase.
- Quedó el registro `PRUEBA_QA_NO_USAR_20261101` para borrar desde Firebase Console.

## Próximo
1. Probar aislamiento entre dos cuentas.
2. Detalle, edición/eliminación, relevamientos, tareas y visitas según alcance del equipo.
3. Offline con persistencia de Firestore al final.

## Decisiones vigentes
- UI Android por ahora; no migrar features a Compose Multiplatform en esta etapa.
- Clean Architecture: dominio define modelos, validación y puertos; data adapta DTO; Firebase queda encapsulado; UI depende del dominio.
- Compose stateless + UDF; ViewModels no contienen strings ni Context; constructor injection desde `AppContainer`.
- Mantener `obras` raíz con `userId`, `nombre`, `cliente`, `publica`, `direccion`, `fechaInicio` Timestamp y `estado` minúsculo.
- No crear `usuarios/{uid}` ni modificar/desplegar reglas. Visibilidad pública no habilita explorar ni seguir obras en este corte.
- “Hoy” se calcula con la zona local; `fechaInicio` se guarda desde medianoche UTC. La hora de visita son minutos desde medianoche.
- Sin fotos en Firebase Storage; se acordó Cloudinary. Sin FCM para avisos locales.
- Errores tipados; cancellation se relanza; no convertir escrituras fallidas en éxito.
