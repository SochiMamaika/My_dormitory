#include "FileRepository.h"
// Создать пользователя в БД
    File FileRepository::createFile(const std::string body, 
                                    const std::list<std::string> file_paths,
                                    int user_id)
{
    auto transaction = db_->newTransaction();
    
    // Создаем запись файла
    auto result = transaction->execSqlSync
    (
        "INSERT INTO files (body, creator_id) "
        "VALUES ($1, $2) "
        "RETURNING id, body, date",
        body, user_id
    );
    
    File file;
    file.fromDb(result[0]);
    int file_id = file.getId();
    
    // Добавляем файлы
    if (!file_paths.empty()) 
    {
        for (const auto& file_path : file_paths) 
        {
            transaction->execSqlSync
            (
                "INSERT INTO files_file (file_id, file_path) "
                "VALUES ($1, $2)",
                file_id, file_path
            );
        }
    }
    file.setFilePaths(file_paths);
    return file;
}

    
// Удаление
bool FileRepository::deleteFile(int id_file)
{
    // Удаляем файлы из MinIO
    S3Service s3service("mydormitory");
    auto files_result = db_->execSqlSync
    (
        "SELECT file_path FROM files_file "
        "WHERE file_id = $1",
        id_file
    );
    
    for (const auto& row : files_result) 
    {
        std::string file_path = row["file_path"].as<std::string>();
        s3service.deleteFile(file_path);
    }
    
    // Удаляем файлы из БД
    db_->execSqlSync
    (
        "DELETE FROM files_file "
        "WHERE file_id = $1",
        id_file
    );
    
    // Удаляем запись файла
    auto result = db_->execSqlSync
    (
        "DELETE FROM files "
        "WHERE id = $1",
        id_file
    );
    return result.affectedRows() > 0;
}


std::list<File> FileRepository::getFiles()
{
    auto tran = db_->newTransaction();
    auto result = tran->execSqlSync
    (
        "SELECT * FROM files "
        "ORDER BY date DESC "
    );
    
    std::list<File> files_all;
    for (const auto& row : result) 
    {
        File file;
        file.fromDb(row);
        int file_id = file.getId();
        // Получаем файлы
        auto files_result = tran->execSqlSync
        (
            "SELECT file_path FROM files_file "
            "WHERE file_id = $1 "
            "ORDER BY file_id",
            file_id
        );
        
        std::list<std::string> file_paths;
        for (const auto& file_row : files_result) 
        {
            file_paths.push_back(file_row["file_path"].as<std::string>());
        }
        file.setFilePaths(file_paths);
        
        files_all.push_back(file);
    }
    return files_all;
}
