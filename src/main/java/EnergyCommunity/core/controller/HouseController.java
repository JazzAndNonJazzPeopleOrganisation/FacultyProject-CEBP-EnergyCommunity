package EnergyCommunity.core.controller;

import EnergyCommunity.core.model.Battery;
import EnergyCommunity.core.model.House;
import EnergyCommunity.core.model.SolarPanel;
import EnergyCommunity.core.service.HouseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class HouseController implements HouseAPI {

    private final HouseService houseService;

    public HouseController(HouseService houseService) {
        this.houseService = houseService;
    }

    @Override
    public ResponseEntity<List<House>> getAllHouses() {
        return ResponseEntity.ok(houseService.findAllHouses());
    }

    @Override
    public ResponseEntity<House> getHouseById(Long id) {
        return ResponseEntity.ok(houseService.findHouseById(id));
    }

    @Override
    public ResponseEntity<List<House>> getHousesByUserId(Long userId) {
        return ResponseEntity.ok(houseService.findHousesByUserId(userId));
    }

    @Override
    public ResponseEntity<House> createHouse(House house) {
        House created = houseService.saveHouse(house);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    public ResponseEntity<House> createHouseForUser(Long userId, House house) {
        House created = houseService.saveHouseForUser(userId, house);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    public ResponseEntity<House> updateHouse(Long id, House house) {
        return ResponseEntity.ok(houseService.updateHouse(id, house));
    }

    @Override
    public ResponseEntity<Void> deleteHouse(Long id) {
        houseService.deleteHouse(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<Battery>> getBatteriesByHouseId(Long houseId) {
        return ResponseEntity.ok(houseService.findBatteriesByHouseId(houseId));
    }

    @Override
    public ResponseEntity<Battery> addBatteryToHouse(Long houseId, Battery battery) {
        Battery created = houseService.addBatteryToHouse(houseId, battery);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    public ResponseEntity<Battery> updateBattery(Long batteryId, Battery battery) {
        return ResponseEntity.ok(houseService.updateBattery(batteryId, battery));
    }

    @Override
    public ResponseEntity<Void> deleteBattery(Long batteryId) {
        houseService.deleteBattery(batteryId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<SolarPanel>> getSolarPanelsByHouseId(Long houseId) {
        return ResponseEntity.ok(houseService.findSolarPanelsByHouseId(houseId));
    }

    @Override
    public ResponseEntity<SolarPanel> addSolarPanelToHouse(Long houseId, SolarPanel solarPanel) {
        SolarPanel created = houseService.addSolarPanelToHouse(houseId, solarPanel);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    public ResponseEntity<SolarPanel> updateSolarPanel(Long panelId, SolarPanel solarPanel) {
        return ResponseEntity.ok(houseService.updateSolarPanel(panelId, solarPanel));
    }

    @Override
    public ResponseEntity<Void> deleteSolarPanel(Long panelId) {
        houseService.deleteSolarPanel(panelId);
        return ResponseEntity.noContent().build();
    }
}
