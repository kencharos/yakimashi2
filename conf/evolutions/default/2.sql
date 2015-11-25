# --- !Ups

insert into T_USER values('user1', '0A041B9462CAA4A31BAC3567E0B6E6FD9100787DB2AB433D96F6D178CABFCE90');

insert into T_LABEL values('A', 'A');
insert into T_LABEL values('B', 'B');
insert into T_LABEL values('C', 'C');
insert into T_LABEL values('D', 'D');
insert into T_LABEL values('E', 'E');
insert into T_LABEL values('F', 'F');
insert into T_LABEL values('G', 'G');
insert into T_LABEL values('H', 'H');

# --- !Downs

delete from T_USER;
delete from T_LABEL;