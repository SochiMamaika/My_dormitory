#include "NewsRepository.h"
News NewsRepository::createNews(const std::string header, 
                                const std::string body, 
                                const std::string author,
                                const std::string date_start, 
                                const std::string date_end,
                                const std::list<std::string> image_paths,
                                int user_id)
{
    auto trans = db_->newTransaction();
    // Вставляем саму новость
    auto result = trans->execSqlSync
    (
        "INSERT INTO news (header, body, author, date_start, date_end, creator_id) "
        "VALUES ($1, $2, $3, $4, $5, $6) "
        "RETURNING id, header, body, author, date, date_start, date_end",
        header, body, author, date_start, date_end, user_id
    );

    News news;
    news.fromDb(result[0]);
    int news_id = news.getId();

    // Вставляем картинки
    if (!image_paths.empty()) 
    {
        for (const auto& image_path : image_paths) 
        {
            trans->execSqlSync
            (
                "INSERT INTO news_file (news_id, image_path) "
                "VALUES ($1, $2)",
                news_id, image_path
            );
        }
    }
    news.setImagePaths(image_paths);
    return news;
}



bool NewsRepository::deleteNews(int id)
{
    auto trans = db_->newTransaction();
    //для начала нужно удалить фотку из minio
    S3Service s3service("mydormitory");

    // 1. Получаем все изображения для удаления из MinIO
    auto images_result = trans->execSqlSync
    (
        "SELECT image_path FROM news_file "
        "WHERE news_id = $1",
        id
    );
    
    for (auto num : images_result)
    {
        std::string image_path = num["image_path"].as<std::string>();
        s3service.deleteFile(image_path);
    }

    
    auto result_2 = trans->execSqlSync
    (
        "DELETE FROM news_file "
        "WHERE news_id = ($1) ", 
        id
    );
    
    
    auto result_3 = trans->execSqlSync
    (
        "DELETE FROM news "
        "WHERE id = ($1) ", 
        id
    );

    return result_3.affectedRows() > 0;
}

std::list<News> NewsRepository::getNews(int limit)
{
    auto tran = db_->newTransaction();
    std::string limit_str = std::to_string(limit);
    auto result = tran->execSqlSync
    (
        "SELECT * FROM news "
        "ORDER BY date DESC "
        "LIMIT $1 ",
        limit_str
    );

    std::list<News> news_all;
    // Шаг 2: Для каждой новости получаем ее изображения
    for (const auto& news_row : result) 
    {
        News news;
        news.fromDb(news_row); // Заполняем основные данные
        
        int news_id = news.getId();
        
        // Шаг 3: Получаем ВСЕ изображения для этой новости
        auto images_result = tran->execSqlSync
        (
            "SELECT image_path FROM news_file "
            "WHERE news_id = $1 "
            "ORDER BY news_id",
            news_id
        );
        
        std::list<std::string> image_paths;
        for (const auto& image_row : images_result) 
        {
            image_paths.push_back(image_row["image_path"].as<std::string>());
        }

        // Шаг 4: Устанавливаем список изображений для новости
        news.setImagePaths(image_paths);

        news_all.push_back(news);
    }

    return news_all;
}

std::list<Repair> NewsRepository::getNewsForRepairman(int limit)
{
    auto tran = db_->newTransaction();
    std::string limit_str = std::to_string(limit);
    auto result = tran->execSqlSync
    (
        "SELECT * FROM repair "
        "ORDER BY date DESC "
        "LIMIT $1 ",
        limit_str
    );

    std::list<Repair> news_all;
    // Шаг 2: Для каждой новости получаем ее изображения
    for (const auto& news_row : result) 
    {
        Repair newsForRepair;
        newsForRepair.fromDb(news_row); // Заполняем основные данные
        
        int newsForRepair_id = newsForRepair.getId();
        
        // Шаг 3: Получаем ВСЕ изображения для этой новости
        auto images_result = tran->execSqlSync
        (
            "SELECT image_path FROM repair_file "
            "WHERE repair_id = $1 "
            "ORDER BY repair_id",
            newsForRepair_id
        );
        
        std::list<std::string> image_paths;
        for (const auto& image_row : images_result) 
        {
            image_paths.push_back(image_row["image_path"].as<std::string>());
        }

        // Шаг 4: Устанавливаем список изображений для новости
        newsForRepair.setRepairPaths(image_paths);

        news_all.push_back(newsForRepair);
    }

    return news_all;
}