#pragma once
#include <string>
#include <list>
#include <memory>
#include "News.h"
#include "Repair.h"
#include "NewsRepository.h"
#include <bcrypt/BCrypt.hpp>
#include <jwt-cpp/jwt.h>
#include <drogon.h>
#include <string>
#include <stdexcept>
#include <iostream>
#include "traits.h"
using traits = jwt::traits::open_source_parsers_jsoncpp;

class NewsService
{

    public:
        // Конструктор
        explicit NewsService(const drogon::orm::DbClientPtr& dbClient);

        News createNews(std::string header,
                        std::string body,
                        std::string author,
                        std::string date_start,
                        std::string date_end,
                        std::list<std::string> image_paths,
                        int user_id);

        bool deleteNews(int id_news);

        std::list<News> getNews(int limit);

        std::list<Repair> getNewsForRepairman(int limit);

    private:
        std::shared_ptr<NewsRepository> repository; // Доступ к БД
};