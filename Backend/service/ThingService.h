#pragma once
#include <string>
#include <list>
#include <memory>
#include "Thing.h"
#include "ThingRepository.h"
#include <bcrypt/BCrypt.hpp>
#include <jwt-cpp/jwt.h>
#include <drogon.h>
#include <stdexcept>
#include <iostream>
#include "traits.h"
using traits = jwt::traits::open_source_parsers_jsoncpp;

class ThingService
{
    public:
        // Конструктор
        explicit ThingService(const drogon::orm::DbClientPtr& dbClient);

        Thing createThing(std::string type,
                          std::string body,
                          int room,
                          std::list<std::string> file_path,
                          int user_id);

        bool deleteThing(int id_thing);

        std::list<Thing> getThings();

    private:
        std::shared_ptr<ThingRepository> repository; // Доступ к БД
};