from locust import HttpUser, task
import uuid
import random

class ApiUser(HttpUser):
    wait_time = lambda self: 1

    def on_start(self):
        username = "user_" + str(uuid.uuid4())
        response = self.client.post("/login", json={"username": username, "password": "test"})
        self.token = response.text

    @task
    def search(self):
        vector = [random.random(), random.random(), random.random()]
        self.client.post(
            "/search",
            json={"query": vector, "k": 1},
            headers={"Authorization": "Bearer " + self.token}
        )


