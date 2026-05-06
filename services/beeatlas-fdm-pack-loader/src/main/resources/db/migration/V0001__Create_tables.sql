/* Create Tables */

CREATE TABLE pack_loader.operation_enum
(
    id integer NOT NULL,
    operation_name varchar(100) NOT NULL,
    queue_name varchar(100) NOT NULL
);

CREATE TABLE pack_loader.package_parts
(
    id SERIAL PRIMARY KEY,
    id_package integer NOT NULL,
    payload text NOT NULL,
    status_id integer NOT NULL,
    part_num integer NOT NULL
);

CREATE TABLE pack_loader.packages
(
    id SERIAL PRIMARY KEY,
    operation_id integer NOT NULL,
    count integer NOT NULL,
    status varchar(50) NOT NULL,
    payload text NULL
);

CREATE TABLE pack_loader.status_enum
(
    id integer NOT NULL,
    status varchar(50) NOT NULL
);

/* Create Primary Keys, Indexes, Uniques, Checks */

ALTER TABLE pack_loader.operation_enum ADD CONSTRAINT pk_operation_enum
    PRIMARY KEY (id);

CREATE INDEX ixfk_package_parts_packages ON pack_loader.package_parts (id_package ASC);

CREATE INDEX ixfk_package_parts_status_enum ON pack_loader.package_parts (status_id ASC);

CREATE INDEX ixfk_packages_operation_enum ON pack_loader.packages (operation_id ASC);

ALTER TABLE pack_loader.status_enum ADD CONSTRAINT pk_status_enum
    PRIMARY KEY (id);

/* Create Foreign Key Constraints */

ALTER TABLE pack_loader.package_parts ADD CONSTRAINT fk_package_parts_packages
    FOREIGN KEY (id_package) REFERENCES packages (id) ON DELETE No Action ON UPDATE No Action;

ALTER TABLE pack_loader.package_parts ADD CONSTRAINT fk_package_parts_status_enum
    FOREIGN KEY (status_id) REFERENCES status_enum (id) ON DELETE No Action ON UPDATE No Action;

ALTER TABLE pack_loader.packages ADD CONSTRAINT fk_packages_operation_enum
    FOREIGN KEY (operation_id) REFERENCES operation_enum (id) ON DELETE No Action ON UPDATE No Action;
