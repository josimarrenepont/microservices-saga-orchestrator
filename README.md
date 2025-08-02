# Projeto de Microsserviços com Saga de Orquestração

## 📚 Visão Geral

Este projeto é uma implementação de um sistema de processamento de pedidos utilizando uma arquitetura de microsserviços. Para garantir a consistência de dados em um ambiente distribuído, empregamos o **padrão de design Saga de Orquestração**, que coordena uma sequência de transações locais através de eventos assíncronos.

A comunicação entre os serviços é realizada de forma robusta e escalável através do **Apache Kafka**. A orquestração de todos os serviços de infraestrutura é feita com **Docker Compose**.

## 🏗️ Arquitetura

A arquitetura do projeto é baseada no padrão de microsserviços com Saga de Orquestração, conforme ilustrado abaixo:

<img width="1466" height="765" alt="Arquitetura Proposta" src="https://github.com/user-attachments/assets/a20e1b92-8baf-4a69-86a4-6e24f6158e3b" />


### A Saga funciona da seguinte forma:
1.  **Order Service** recebe um pedido e inicia a Saga, publicando um evento no Kafka.
2.  O **Orchestrator** escuta o evento de início da Saga e coordena os próximos passos, enviando eventos para os outros serviços.
3.  **Product Validation Service**, **Payment Service** e **Inventory Service** processam suas transações locais e respondem ao Orchestrator com eventos de sucesso ou falha.
4.  Em caso de sucesso, o Orchestrator continua a Saga.
5.  Em caso de falha em qualquer passo, o Orchestrator inicia a **transação de compensação** (rollback), enviando eventos para desfazer as operações já realizadas pelos serviços.

## 🛠️ Tecnologias Utilizadas

* **Microsserviços:** Spring Boot
* **Mensageria:** Apache Kafka
* **Orquestração de Contêineres:** Docker e Docker Compose
* **Bancos de Dados:**
    * MongoDB (para o `Order Service`)
    * PostgreSQL (para `Product Validation`, `Payment` e `Inventory` Services)

## 📦 Serviços da Aplicação

O projeto é composto pelos seguintes microsserviços Spring Boot:

| Serviço                      | Descrição                                                                      | Tecnologias             |
| ---------------------------- | ------------------------------------------------------------------------------ | ----------------------- |
| `order-service`              | Recebe novos pedidos e inicia o processo da Saga.                              | Spring Boot, MongoDB    |
| `orchestrator`               | Coordena o fluxo da Saga e lida com falhas e compensações.                      | Spring Boot, Kafka      |
| `product-validation-service` | Valida a disponibilidade do produto no estoque.                                  | Spring Boot, PostgreSQL |
| `payment-service`            | Processa o pagamento do pedido.                                                | Spring Boot, PostgreSQL |
| `inventory-service`          | Gerencia a atualização do inventário após o pagamento.                          | Spring Boot, PostgreSQL |

## 🚀 Como Executar o Projeto

Siga os passos abaixo para configurar e executar o projeto em seu ambiente local.

### Pré-requisitos

* **Docker Desktop** (com Docker Compose incluído)
* **JDK 17+** (ou a versão utilizada pelo seu projeto)
* **Maven** ou **Gradle**

### 1. Iniciar a Infraestrutura com Docker Compose

Navegue até a raiz do projeto no seu terminal e execute o seguinte comando. Ele irá construir e iniciar todos os contêineres necessários (bancos de dados, Kafka e Zookeeper).

```bash
docker-compose up --build -d
```
