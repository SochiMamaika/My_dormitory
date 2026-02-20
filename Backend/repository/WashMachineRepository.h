#pragma once
#include <DbClient.h> // Подключение к PostgreSQL
#include <string>
#include "WashMachine.h"
#include "ReserveWashMachine.h"
#include "ReserveWashMachineRepository.h"
#include <list>
#include "bcrypt/BCrypt.hpp"
class WashMachineRepository
{
    public:
        explicit WashMachineRepository(const drogon::orm::DbClientPtr& dbClient) : db_(dbClient) {}

        std::list<WashMachine> getWashMachines();

        bool deleteWashMachine(int id);

        void addWashMachine(const std::string &name,
                            int user_id);

        std::list<ReserveWashMachine> getReserveWashMachines();

        bool deleteReserveWashMachine(int id);

    private:
        drogon::orm::DbClientPtr db_; // Подключение к PostgreSQL
};