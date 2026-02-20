#include "TutorRepository.h"
// Создать пользователя в БД
Tutor TutorRepository::createTutor(const std::string header, 
                                    const std::string body, 
                                    const std::list<std::string> image_paths,
                                    int user_id)
{
    auto transaction = db_->newTransaction();
    
    // Создаем преподавателя
    auto result = transaction->execSqlSync
    (
        "INSERT INTO tutor (header, body, creator_id) "
        "VALUES ($1, $2, $3) "
        "RETURNING id, header, body, date",
        header, body, user_id
    );
    
    Tutor tutor;
    tutor.fromDb(result[0]);
    int tutor_id = tutor.getId();
    
    // Добавляем изображения
    if (!image_paths.empty()) 
    {
        for (const auto& image_path : image_paths) 
        {
            transaction->execSqlSync(
                "INSERT INTO tutor_file (tutor_id, image_path) "
                "VALUES ($1, $2)",
                tutor_id, image_path
            );
        }
    }
    tutor.setImagePaths(image_paths);
    return tutor;
        
}


std::list<Tutor> TutorRepository::getTutor()
{
    auto tran = db_->newTransaction();
    auto result = tran->execSqlSync
    (
        "SELECT * FROM tutor "
        "ORDER BY date DESC "
    );

    std::list<Tutor> tutor_all;
    for (const auto& row : result) 
    {
        Tutor tutor;
        tutor.fromDb(row);
    
        // Получаем изображения
        auto images_result = tran->execSqlSync
        (
            "SELECT image_path FROM tutor_file "
            "WHERE tutor_id = $1 "
            "ORDER BY tutor_id",
            tutor.getId()
        );

        std::list<std::string> image_paths;
        for (const auto& image_row : images_result) 
        {
            image_paths.push_back(image_row["image_path"].as<std::string>());
        }
        tutor.setImagePaths(image_paths);
        
        tutor_all.push_back(tutor);
    }
    return tutor_all;
}

bool TutorRepository::deleteTutor(int id_tutor)
{
    auto tran = db_->newTransaction();
    S3Service s3service("mydormitory");

    auto images_result = tran->execSqlSync
    (
        "SELECT image_path FROM tutor_file "
        "WHERE tutor_id = $1",
        id_tutor
    );
    
    for (const auto& row : images_result) 
    {
        std::string image_path = row["image_path"].as<std::string>();
        s3service.deleteFile(image_path);
    }
    
    tran->execSqlSync
    (
        "DELETE FROM tutor_file "
        "WHERE tutor_id = $1",
        id_tutor
    );
    
    auto result = tran->execSqlSync
    (
        "DELETE FROM tutor "
        "WHERE id = $1",
        id_tutor
    );
    return result.affectedRows() > 0;
}