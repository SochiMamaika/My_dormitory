#pragma once
#include <DbClient.h> // Подключение к PostgreSQL
#include <string>
#include "News.h"
#include "Repair.h"
#include "S3Controller.h"
#include <list>
class NewsRepository 
{
public:
    // Конструктор принимает подключение к БД
    NewsRepository(const drogon::orm::DbClientPtr &dbClient) : db_(dbClient) {}

    // Создать пользователя в БД
    News createNews(const std::string header, 
                    const std::string body, 
                    const std::string author,
                    const std::string date_start, 
                    const std::string date_end,
                    const std::list<std::string> image_paths,
                    int user_id);
    
    // Удаление
    bool deleteNews(int id);

    std::list<News> getNews(int limit);

    std::list<Repair> getNewsForRepairman(int limit);

private:
    drogon::orm::DbClientPtr db_; // Подключение к PostgreSQL
};