import os
import threading

threads = []


def build_application(app):
    threads.append(app)
    print(f"Building application {app}")
    os.system(f"cd {app} && gradle build -x test")
    print(f"Application {app} finished building!")
    threads.remove(app)


def docker_compose_up():
    print("Running containers!")
    os.system("docker-compose up --build -d")
    print("Pipeline finished!")


def build_all_applications():
    print("Starting to build applications!")

    threading.Thread(target=build_application, args=("order-service",)).start()
    threading.Thread(target=build_application, args=("orchestrator-service",)).start()
    threading.Thread(target=build_application, args=("product-validation-service",)).start()
    threading.Thread(target=build_application, args=("payment-service",)).start()
    threading.Thread(target=build_application, args=("inventory-service",)).start()


def remove_remaining_containers():
    print("Removing all containers.")
    os.system("docker-compose down")

    containers = os.popen("docker ps -aq").read().splitlines()
    if containers:
        print(f"There are still {len(containers)} containers created")
        for container in containers:
            print(f"Stopping container {container}")
            os.system(f"docker container stop {container}")
        os.system("docker container prune -f")


if __name__ == "__main__":
    print("Pipeline started!")
    build_all_applications()

    while len(threads) > 0:
        pass

    remove_remaining_containers()
    threading.Thread(target=docker_compose_up).start()
