ALTER TABLE "administration$account" ADD "birthdate" TIMESTAMP NULL;
ALTER TABLE "administration$account" ADD "surname" VARCHAR_IGNORECASE(200) NULL;
ALTER TABLE "administration$account" ADD "firstname" VARCHAR_IGNORECASE(200) NULL;
ALTER TABLE "administration$account" ADD "gender" VARCHAR_IGNORECASE(6) NULL;
INSERT INTO "mendixsystem$attribute" ("id", 
"entity_id", 
"attribute_name", 
"column_name", 
"type", 
"length", 
"default_value", 
"is_auto_number")
 VALUES ('8d4415f9-2269-4df2-b387-8e4352fd4670', 
'3078a23e-13b2-4a9b-84e4-2881fdee53c6', 
'BirthDate', 
'birthdate', 
20, 
0, 
'', 
false);
INSERT INTO "mendixsystem$attribute" ("id", 
"entity_id", 
"attribute_name", 
"column_name", 
"type", 
"length", 
"default_value", 
"is_auto_number")
 VALUES ('774f1706-63bb-4cb7-afb8-1b6710009197', 
'3078a23e-13b2-4a9b-84e4-2881fdee53c6', 
'Gender', 
'gender', 
40, 
6, 
'', 
false);
INSERT INTO "mendixsystem$attribute" ("id", 
"entity_id", 
"attribute_name", 
"column_name", 
"type", 
"length", 
"default_value", 
"is_auto_number")
 VALUES ('43790dd9-62b7-4d25-a181-7f1548226bdf', 
'3078a23e-13b2-4a9b-84e4-2881fdee53c6', 
'SurName', 
'surname', 
30, 
200, 
'', 
false);
INSERT INTO "mendixsystem$attribute" ("id", 
"entity_id", 
"attribute_name", 
"column_name", 
"type", 
"length", 
"default_value", 
"is_auto_number")
 VALUES ('cb693e66-b2d4-452c-a76b-d92f4cd053a1', 
'3078a23e-13b2-4a9b-84e4-2881fdee53c6', 
'FirstName', 
'firstname', 
30, 
200, 
'', 
false);
UPDATE "mendixsystem$version"
 SET "versionnumber" = '4.2', 
"lastsyncdate" = '20211017 16:14:23';
