#pragma once
#include <DbClient.h>
#include <string>
#include "S3Controller.h"
#include "Tutor.h"
#include <list>
#include "bcrypt/BCrypt.hpp"
class TutorRepository 
{
    public:
        TutorRepository(const drogon::orm::DbClientPtr &dbClient) : db_(dbClient) {}

        Tutor createTutor(const std::string header, 
                          const std::string body, 
                          const std::list<std::string> image_path,
                          int user_id);
        
        bool deleteTutor(int id_tutor);

        std::list<Tutor> getTutor();

    private:
        drogon::orm::DbClientPtr db_;
};