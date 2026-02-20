#pragma once
#include <string>
#include <list>
#include <memory>
#include "WashMachine.h"
#include "WashMachineRepository.h"
#include <bcrypt/BCrypt.hpp>
#include <jwt-cpp/jwt.h>
#include <drogon.h>
#include <stdexcept>
#include <iostream>
#include "traits.h"
using traits = jwt::traits::open_source_parsers_jsoncpp;

class WashMachineService
{
    public:
        // Конструктор
        explicit WashMachineService(const drogon::orm::DbClientPtr& dbClient);
            
        std::list<WashMachine> getWashMachines();

        void addWashMachine(const std::string name,
                            int user_id);

        bool deleteWashMachine(int id);

    private:
        std::shared_ptr<WashMachineRepository> repository;
};