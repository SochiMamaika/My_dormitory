#include "NewsService.h"

// Конструктор — инициализируем репозиторий
NewsService::NewsService(const drogon::orm::DbClientPtr& dbClient)
{
    repository = std::make_shared<NewsRepository>(dbClient);
}
News NewsService::createNews(std::string header,
                             std::string body,
                             std::string author,
                             std::string date_start,
                             std::string date_end,
                             std::list<std::string> image_paths,
                             int user_id)
                             
{

    if (image_paths.size() >= 5)
    {
        throw std::runtime_error("Count files should be < 5");
    }

    if (date_start > date_end)
    {
        throw std::runtime_error("Date start must be bigger that date end");
    }

    return repository->createNews(header,
                                  body,
                                  author,
                                  date_start,
                                  date_end,
                                  image_paths,
                                  user_id);
}

bool NewsService::deleteNews(int id_news)
{
    return repository->deleteNews(id_news);
}

std::list<News> NewsService::getNews(int limit)
{
    return repository->getNews(limit);
}

std::list<Repair> NewsService::getNewsForRepairman(int limit)
{
    return repository->getNewsForRepairman(limit);
}
