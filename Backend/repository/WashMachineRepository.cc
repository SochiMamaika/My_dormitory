#include "WashMachineRepository.h"

std::list<WashMachine> WashMachineRepository::getWashMachines()
{
    auto result = db_->execSqlSync
    (
        "SELECT * FROM machines"
    );
    auto dbClient = drogon::app().getDbClient();
    ReserveWashMachineRepository reserve(dbClient);
    auto reservations = reserve.getReserveWashMachines();
    
    // Создаем хэш-таблицу для быстрого поиска резерваций по machine_id
    std::unordered_map<int, std::list<ReserveWashMachine>> reservationsMap;
    
    // Группируем резервации по machine_id
    for (auto& reservation : reservations) 
    {
        reservationsMap[reservation.getMachineId()].push_back(reservation);
    }
    
    // Создаем список машин с их резервациями
    std::list<WashMachine> machines;
    for (auto& row : result) 
    {
        WashMachine washmachine;
        washmachine.FromDB(row);
        int machineId = washmachine.getId();
        auto it = reservationsMap.find(machineId);
        if (it != reservationsMap.end()) 
        {
            washmachine.setReserve(it->second);
        }
        
        machines.push_back(washmachine);
    }
    return machines;
}

bool WashMachineRepository::deleteWashMachine(int id)
{
    auto result = db_->execSqlSync
    (
        "DELETE FROM machines "
        "WHERE id = $1", id
    );

    auto result_2 = db_->execSqlSync
    (
        "DELETE FROM bookings "
        "WHERE machine_id = $1", id
    );
    return result.affectedRows() > 0;
}

void WashMachineRepository::addWashMachine(const std::string &name,
                                            int user_id)
{
    db_->execSqlSync
    (
        "INSERT INTO machines(name, creator_id) "
        "VALUES($1, $2)", 
        name, user_id
    );
}