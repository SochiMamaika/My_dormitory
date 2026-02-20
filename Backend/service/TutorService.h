#pragma once
#include <string>
#include <list>
#include <memory>
#include "Tutor.h"
#include "TutorRepository.h"
#include <bcrypt/BCrypt.hpp>
#include <jwt-cpp/jwt.h>
#include <drogon/drogon.h>
#include <stdexcept>
#include <iostream>
#include "traits.h"
using traits = jwt::traits::open_source_parsers_jsoncpp;

class TutorService
{
    public:
        // Конструктор
        explicit TutorService(const drogon::orm::DbClientPtr& dbClient);

        Tutor createTutor(std::string header,
                          std::string body,
                          std::list<std::string> image_path,
                          int user_id);

        bool deleteTutor(int id_tutor);

        std::list<Tutor> getTutor();

    private:
        std::shared_ptr<TutorRepository> repository; // Доступ к БД
};