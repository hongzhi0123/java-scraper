-- tpps table
CREATE TABLE tpps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    authId VARCHAR(255),
    natId VARCHAR(255)
);

-- roles table (lookup)
CREATE TABLE roles (
    name VARCHAR(50) PRIMARY KEY  -- e.g., 'READ', 'WRITE', 'ADMIN'
);

-- join table (many-to-many)
CREATE TABLE tpp_roles (
    tpp_id BIGINT NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (tpp_id, role_name),
    FOREIGN KEY (tpp_id) REFERENCES tpps(id) ON DELETE CASCADE,
    FOREIGN KEY (role_name) REFERENCES roles(name)
);