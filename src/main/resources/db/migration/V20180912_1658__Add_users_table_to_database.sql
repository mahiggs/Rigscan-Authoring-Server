CREATE TABLE roles (
  role NVARCHAR(255) NOT NULL PRIMARY KEY
);

INSERT INTO roles (role) VALUES ('UPLOADER');
INSERT INTO roles (role) VALUES ('ADMINISTRATOR');
INSERT INTO roles (role) VALUES ('USER');
INSERT INTO roles (role) VALUES ('LITE_USER');

CREATE TABLE users (
  id          INT           NOT NULL PRIMARY KEY,
  user_login  nvarchar(255) NOT NULL,
  user_name   nvarchar(255) NOT NULL,
  created_at  datetime      NOT NULL,
  modified_at datetime      NOT NULL,
);

CREATE TABLE user_roles (
  user_id   INT           NOT NULL,
  user_role nvarchar(255) NOT NULL,
  foreign key (user_id) references users (id),
  foreign key (user_role) references roles (role)
)