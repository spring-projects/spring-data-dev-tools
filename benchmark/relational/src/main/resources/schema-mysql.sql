drop table if exists Book;
create table Book (
	id INT NOT NULL AUTO_INCREMENT,
	title varchar(255),
	pages integer not null,
	PRIMARY KEY (id)
);
