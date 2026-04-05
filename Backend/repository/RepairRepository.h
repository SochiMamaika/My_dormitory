#pragma once
#include <DbClient.h> // Подключение к PostgreSQL
#include <string>
#include "S3Controller.h"
#include "Repair.h"
#include <list>

class RepairRepository 
{
    public:
        // Конструктор принимает подключение к БД
        RepairRepository(const drogon::orm::DbClientPtr &dbClient) : db_(dbClient) {}

        // Создать пользователя в БД
        Repair createRepair(const std::string type, 
                            const std::string body, 
                            int room,
                            std::list<std::string> repair_paths,
                            int user_id);

        bool changeActivateRepair(int id,
                                  bool activity,
                                  int repairman_id);

        bool changeEndingRepair(int id,
                                bool ending,
                                int repairman_id);
        
        // Удаление
        bool deleteRepair(int id_repair);

        std::list<Repair> getRepairs();

        std::list<Repair> getMyRepairs(int user_id, std::string user_type);

    private:
        drogon::orm::DbClientPtr db_; // Подключение к PostgreSQL
};