#pragma once
#include <DbClient.h> // Подключение к PostgreSQL
#include <string>
#include "S3Controller.h"
#include "Thing.h"
#include <list>
class ThingRepository 
{
    public:
        // Конструктор принимает подключение к БД
        ThingRepository(const drogon::orm::DbClientPtr &dbClient) : db_(dbClient) {}

        // Создать пользователя в БД
        Thing createThing(const std::string type, 
                          const std::string body, 
                          int room,
                          std::list<std::string> thing_paths,
                          int user_id);
        
        // Удаление
        bool deleteThing(int id_tutor);

        std::list<Thing> getThings();

    private:
        drogon::orm::DbClientPtr db_; // Подключение к PostgreSQL
};