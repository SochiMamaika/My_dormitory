#pragma once
#include <string>
#include <list>
#include <memory>
#include "Repair.h"
#include "RepairRepository.h"
#include <bcrypt/BCrypt.hpp>
#include <jwt-cpp/jwt.h>
#include <drogon/drogon.h>
#include <stdexcept>
#include <iostream>
#include "traits.h"
using traits = jwt::traits::open_source_parsers_jsoncpp;

class RepairService
{

    public:
        // Конструктор
        explicit RepairService(const drogon::orm::DbClientPtr& dbClient);

        Repair createRepair(std::string type,
                            std::string body,
                            int room,
                            std::list<std::string> repair_path,
                            int user_id);

        
        bool changeActivateRepair(int id,
                                  bool activity,
                                  int repairman_id);

        bool changeEndingRepair(int id,
                                bool ending,
                                int repairman_id);

        bool deleteRepair(int id_repair);

        std::list<Repair> getRepairs();

        std::list<Repair> getMyRepairs(int user_id, std::string user_type);

    private:
        std::shared_ptr<RepairRepository> repository; // Доступ к БД
};