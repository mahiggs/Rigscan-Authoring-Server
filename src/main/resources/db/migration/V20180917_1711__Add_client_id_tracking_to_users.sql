CREATE TABLE user_clients (
user_id INT NOT NULL,
client_id VARCHAR(50) NOT NULL,
FOREIGN KEY (user_id) REFERENCES users (id),
PRIMARY KEY (user_id, client_id)
)