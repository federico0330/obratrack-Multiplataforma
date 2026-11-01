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

## Detalle, edición y estado de obras
- Se agregó el detalle de una obra propia, la edición de sus campos básicos y el cambio entre activa, pausada y finalizada.
- Las escrituras son parciales: conservan `userId` y campos que el formulario no edita; la fecha continúa guardándose como Timestamp y los estados en minúscula.
- Al cambiar la sesión, se cancela la consulta anterior y se limpia el detalle/formulario antes de cargar la lista del nuevo UID.
- Tests de domain, data, Firebase y `features:obras` pasan; `:androidApp:assembleDebug` pasa.
- En emulador se revisaron el listado, detalle y formulario con los valores existentes. No se guardaron cambios ni se alteraron datos compartidos durante esa revisión.
- El aislamiento está cubierto en pruebas del ViewModel y repositorio; falta comprobarlo manualmente con dos cuentas reales y contra las reglas publicadas.

## Baja de obras
- La eliminación pide confirmación y verifica en servidor que la obra pertenezca a la sesión actual.
- Se borran primero las tareas, relevamientos y suscriptores en lotes; la obra se borra al final. Si falla después de borrar datos relacionados, se informa que el resultado puede ser parcial.
- Se agregaron tests de dominio, repositorio y ViewModel, incluyendo que cancelar no invoque la baja.
- Pasan los tests de domain, data, firebase y `features:obras`, y `:androidApp:assembleDebug`.
- En emulador se revisaron el detalle y el diálogo de confirmación; se canceló la baja y no se modificaron datos de Firebase.

## Próximo
1. Verificar manualmente el aislamiento con dos cuentas y las reglas publicadas, sin modificarlas.
2. Preparar y compartir login y obras en la primera app iOS.
3. Después seguir con relevamientos y tareas; dejar offline para el final.

## Decisiones vigentes
- UI Android por ahora; no migrar features a Compose Multiplatform en esta etapa.
- Clean Architecture: dominio define modelos, validación y puertos; data adapta DTO; Firebase queda encapsulado; UI depende del dominio.
- Compose stateless + UDF; ViewModels no contienen strings ni Context; constructor injection desde `AppContainer`.
- Mantener `obras` raíz con `userId`, `nombre`, `cliente`, `publica`, `direccion`, `fechaInicio` Timestamp y `estado` minúsculo.
- No crear `usuarios/{uid}` ni modificar/desplegar reglas. Visibilidad pública no habilita explorar ni seguir obras en este corte.
- “Hoy” se calcula con la zona local; `fechaInicio` se guarda desde medianoche UTC. La hora de visita son minutos desde medianoche.
- Sin fotos en Firebase Storage; se acordó Cloudinary. Sin FCM para avisos locales.
- Errores tipados; cancellation se relanza; no convertir escrituras fallidas en éxito.
