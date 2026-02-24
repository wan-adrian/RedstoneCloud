<div align="center">
<img src="https://avatars.githubusercontent.com/u/178515769?s=400&u=a56cd5675db6ef4014913e34ff55ed0d3e5013d2&v=4" width="150" height="150" alt="RedstoneCloud">
<h4>Revolutionary and cutting-edge Cloud technology for Minecraft</h4>
</div>

## 📖 Overview
RedstoneCloud is a state-of-the-art server management platform designed for Minecraft networks. Regardless of the software you use, RedstoneCloud is built to support it all.

## 🎯 Key Features
* Offers a highly flexible server management system to accommodate any software.
* Includes a simple and intuitive API for ease of use.
* Utilizes a built-in [Redis](https://redis.io/) instance to synchronize data across multiple servers.

## ✨ Extending with Plugins
Want to add more functionality? You can easily develop plugins for RedstoneCloud.

Check out https://github.com/RedstoneCloud/ExamplePlugin for more details.

## 🚀 Getting Started
Download the latest version of RedstoneCloud and run it with the following command:
```bash
java -jar redstonecloud.jar
```
The cloud will generate a basic configuration structure automatically.

Ensure that you have the latest version of our [Bridge](https://github.com/RedstoneCloud/CloudBridge) installed on your server to establish a connection to the cloud.

## 🔌 Optional REST API
RedstoneCloud can expose an optional REST API for external tooling and dashboards.

Enable it in `config.yml`:
```yml
restApi:
  enabled: true
  host: "127.0.0.1"
  port: 8080
  tokens:
    - name: "admin"
      token: "REPLACE_WITH_LONG_RANDOM_TOKEN"
      permissions:
        - "*"
      enabled: true
```

Authentication:
* `Authorization: Bearer <token>` (recommended)
* `X-Api-Token: <token>`

Permission keys:
* `cloud.read`
* `cloud.player.read`
* `cloud.server.manage`
* `cloud.server.execute`
* `*` (full access)

Main routes (`/api/v1`):
* `GET /health`
* `GET /me`
* `GET /servers`
* `GET /servers/{name}`
* `POST /servers/start`
* `POST /servers/{name}/stop`
* `POST /servers/{name}/kill`
* `POST /servers/{name}/execute`
* `GET /players`
* `GET /templates`

## 🙌 How to Contribute
We encourage contributions to help improve RedstoneCloud! Here's how you can get involved:

### Reporting bugs
If you encounter any issues while using RedstoneCloud, please open an issue on GitHub. Be sure to provide a detailed description of the problem, steps to reproduce it, and any relevant logs (excluding sensitive information).

### Submitting a Pull Request
We welcome code contributions! If you've fixed a bug or added a new feature, feel free to submit a pull request. Ensure that your code adheres to our guidelines and includes tests where applicable.

## 📌 Licensing information
This project is licensed under the Apache-2.0 License. For more information, see the [LICENSE](/LICENSE) file.
