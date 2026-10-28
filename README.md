# ObraTrack Multiplataforma

Aplicación multiplataforma para gestión de obras, relevamientos de campo y tareas, desarrollada con Kotlin Multiplatform.

La lógica compartida vive en `shared/`. La guía local de configuración y aprendizaje está en `guia.md` (excluida de Git).

## Arquitectura inicial

`shared/src/commonMain` contiene dominio puro. Las reglas no conocen Firebase ni la interfaz. Los casos de uso coordinan acciones del dominio y dependen de contratos; adaptadores de datos concretos se incorporarán en una etapa posterior. `commonTest` mantiene pruebas que validan reglas compartidas.

## Estado

Primera rebanada: modelo de obra, regla de validación del nombre, caso de uso de creación y pruebas unitarias comunes.
