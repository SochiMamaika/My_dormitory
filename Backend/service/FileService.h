#pragma once
#include <string>
#include <list>
#include <memory>
#include "File.h"
#include "FileRepository.h"
#include <jwt-cpp/jwt.h>
#include <drogon.h>
#include <stdexcept>
#include <iostream>
#include "traits.h"
using traits = jwt::traits::open_source_parsers_jsoncpp;

class FileService
{
public:
    // Конструктор
    explicit FileService(const drogon::orm::DbClientPtr& dbClient);

    File createFile(std::string body,
                    std::list<std::string> file_path,
                    int user_id);

    bool deleteFile(int id_file);

    std::list<File> getFiles();

private:
    std::shared_ptr<FileRepository> repository; // Доступ к БД
};