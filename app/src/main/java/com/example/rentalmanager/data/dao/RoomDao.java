package com.example.rentalmanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.rentalmanager.data.entity.RoomEntity;
import com.example.rentalmanager.data.model.RoomWithTenant;
import java.util.List;

@Dao
public interface RoomDao {

    // INSERT
    @Insert
    void insert(RoomEntity room);

    // UPDATE
    @Update
    void update(RoomEntity room);

    // DELETE
    @Delete
    void delete(RoomEntity room);

    // GET ALL (LiveData để dùng với ViewModel)
    @Query("SELECT * FROM rooms ORDER BY roomName ASC")
    LiveData<List<RoomEntity>> getAllRooms();

    @Query("SELECT * FROM rooms ORDER BY roomName ASC")
    List<RoomEntity> getAllRoomsSync();

    // GET BY ID
    @Query("SELECT * FROM rooms WHERE roomId = :id LIMIT 1")
    RoomEntity getRoomById(int id);

    @Query("SELECT * FROM rooms WHERE roomId = :id LIMIT 1")
    RoomEntity getRoomByIdSync(int id);

    // COUNT TOTAL ROOMS
    @Query("SELECT COUNT(*) FROM rooms")
    LiveData<Integer> countTotalRooms();

    // COUNT OCCUPIED ROOMS
    @Query("SELECT COUNT(*) FROM rooms WHERE status = 'DANG_THUE'")
    LiveData<Integer> countOccupiedRooms();

    @Query("SELECT COUNT(*) FROM rooms")
    int countTotalRoomsSync();

    @Query("SELECT COUNT(*) FROM rooms WHERE status = 'DANG_THUE'")
    int countOccupiedRoomsSync();

    // COUNT EMPTY ROOMS
    @Query("SELECT COUNT(*) FROM rooms WHERE status = 'TRONG'")
    LiveData<Integer> countEmptyRooms();

    @Query("SELECT COUNT(*) FROM rooms WHERE roomName = :name")
    int countByName(String name);

    @Query("UPDATE rooms SET status = :status WHERE roomId = :roomId")
    void updateStatus(int roomId, String status);

    @Query("SELECT * FROM rooms WHERE status = 'TRONG' ORDER BY roomName ASC")
    LiveData<List<RoomEntity>> getAvailableRooms();

    @Query(
            "SELECT rooms.*, " +
                    "contracts.contractId AS c_contractId, " +
                    "contracts.tenantId AS c_tenantId, " +
                    "contracts.startDate AS c_startDate, " +
                    "contracts.endDate AS c_endDate, " +
                    "contracts.status AS c_status, " +
                    "tenants.fullName AS t_fullName " +
                    "FROM rooms " +
                    "LEFT JOIN contracts ON rooms.roomId = contracts.roomId " +
                    "AND contracts.status = 'HIEU_LUC' " +
                    "LEFT JOIN tenants ON contracts.tenantId = tenants.tenantId " +
                    "ORDER BY rooms.roomName ASC"
    )
    LiveData<List<RoomWithTenant>> getRoomsWithTenant();

    @Query(
            "SELECT rooms.*, " +
                    "contracts.contractId AS c_contractId, " +
                    "contracts.tenantId AS c_tenantId, " +
                    "contracts.startDate AS c_startDate, " +
                    "contracts.endDate AS c_endDate, " +
                    "contracts.status AS c_status, " +
                    "tenants.fullName AS t_fullName " +
                    "FROM rooms " +
                    "LEFT JOIN contracts ON rooms.roomId = contracts.roomId " +
                    "AND contracts.status = 'HIEU_LUC' " +
                    "LEFT JOIN tenants ON contracts.tenantId = tenants.tenantId " +
                    "ORDER BY rooms.roomName ASC"
    )
    List<RoomWithTenant> getRoomsWithTenantSync();

}