# Classloading and Resources

## Classloading

Each plugin JAR is loaded with a dedicated `PluginClassLoader`.

## Resource Access

Plugins can access resources inside their JAR via `getResourceFile(String)`.

## Data Folder

On first load, a plugin data folder is created at:

`<workingDir>/plugins/<pluginName>/`
