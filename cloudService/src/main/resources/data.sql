
INSERT INTO users (login, password)
SELECT 'user1', '$2a$10$H/BFYpGYif9/TJreA0koE.4TCY.b19Z8VESqAlNseBaMJ6MHN3fBW'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE login = 'user1');

INSERT INTO users (login, password)
SELECT 'user2', '$2a$10$t1CCCmNJSSHW0UN2WibpLe87077Lhk4vfMAMz6PN7bm4kANMdZq8y'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE login = 'user2');

