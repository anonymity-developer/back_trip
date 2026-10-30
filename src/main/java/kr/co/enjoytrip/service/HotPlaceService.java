package kr.co.enjoytrip.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import jakarta.servlet.http.Part;
import kr.co.enjoytrip.config.AppConfig;
import kr.co.enjoytrip.dao.HotPlaceDao;
import kr.co.enjoytrip.dto.HotPlaceDto;

public class HotPlaceService {
    private static final Logger LOG=Logger.getLogger(HotPlaceService.class.getName());
    private final HotPlaceDao dao=new HotPlaceDao();
    public List<HotPlaceDto> list()throws SQLException{return dao.list();}
    public HotPlaceDto find(long id)throws SQLException{return dao.find(id);}
    public HotPlaceDto owned(long id,Long viewer)throws SQLException{
        HotPlaceDto place=dao.find(id);
        if(place==null||viewer==null||place.userId()!=viewer)throw new ValidationException("수정 권한이 없습니다.");return place;
    }
    public long create(Long user,String title,String place,String type,String date,String description,String lat,String lng,Part image)throws Exception{
        if(user==null)throw new ValidationException("로그인이 필요합니다.");
        Input in=input(title,place,type,date,description,lat,lng);
        String stored=save(image);
        try{return dao.insert(user,in.title,in.place,in.type,in.date,in.description,in.lat,in.lng,stored);}catch(Exception e){remove(stored);throw e;}
    }
    public void update(long id,Long user,String title,String place,String type,String date,String description,String lat,String lng,Part image)throws Exception{
        HotPlaceDto old=owned(id,user);Input in=input(title,place,type,date,description,lat,lng);
        String stored=image==null||image.getSize()==0?old.imagePath():save(image);
        try{
            if(!dao.update(id,user,in.title,in.place,in.type,in.date,in.description,in.lat,in.lng,stored))throw new ValidationException("수정할 수 없습니다.");
            if(!stored.equals(old.imagePath()))remove(old.imagePath());
        }catch(Exception e){if(!stored.equals(old.imagePath()))remove(stored);throw e;}
    }
    public void delete(long id,Long user)throws SQLException,IOException{
        HotPlaceDto old=owned(id,user);if(dao.delete(id,user))remove(old.imagePath());
    }
    public void deleteUserPhotos(List<String> names){
        for(String name:names){
            if(name==null||!name.matches("[0-9a-fA-F-]{36}\\.(jpg|png|webp)"))continue;
            try{remove(name);}catch(IOException e){LOG.warning("Could not remove orphaned Hot Place image: "+name);}
        }
    }
    public Path photoPath(HotPlaceDto place){
        Path dir=root(),file=dir.resolve(place.imagePath()).normalize();
        if(!file.startsWith(dir))throw new IllegalArgumentException("Invalid photo path");
        return file;
    }
    private Input input(String title,String place,String type,String date,String description,String lat,String lng){
        if(title==null||title.isBlank()||title.length()>200||place==null||place.isBlank()||place.length()>200||type==null||type.isBlank()||type.length()>80||description==null||description.isBlank()||description.length()>20000)
            throw new ValidationException("장소 정보와 설명을 확인하세요.");
        try{
            LocalDate day=LocalDate.parse(date);if(day.isAfter(LocalDate.now(ZoneId.of("Asia/Seoul"))))throw new ValidationException("방문일은 오늘 이전이어야 합니다.");
            Double latitude=coordinate(lat,-90,90),longitude=coordinate(lng,-180,180);
            return new Input(title.trim(),place.trim(),type.trim(),day,description.trim(),latitude,longitude);
        }catch(DateTimeParseException|NullPointerException e){throw new ValidationException("방문일을 확인하세요.");}
    }
    private Double coordinate(String value,int min,int max){if(value==null||value.isBlank())return null;try{double n=Double.parseDouble(value);if(!Double.isFinite(n)||n<min||n>max)throw new NumberFormatException();return n;}catch(NumberFormatException e){throw new ValidationException("지도 좌표를 확인하세요.");}}
    private String save(Part image)throws IOException{
        if(image==null||image.getSize()==0)throw new ValidationException("사진을 선택하세요.");
        if(image.getSize()>5_000_000)throw new ValidationException("사진은 5MB 이하여야 합니다.");
        byte[] bytes=image.getInputStream().readAllBytes();String extension;
        if(bytes.length>=3&&(bytes[0]&255)==255&&(bytes[1]&255)==216&&(bytes[2]&255)==255)extension="jpg";
        else if(bytes.length>=8&&(bytes[0]&255)==137&&bytes[1]==80&&bytes[2]==78&&bytes[3]==71)extension="png";
        else if(bytes.length>=12&&bytes[0]=='R'&&bytes[1]=='I'&&bytes[2]=='F'&&bytes[3]=='F'&&bytes[8]=='W'&&bytes[9]=='E'&&bytes[10]=='B'&&bytes[11]=='P')extension="webp";
        else throw new ValidationException("JPG, PNG, WebP 이미지만 업로드할 수 있습니다.");
        String mime=image.getContentType();
        if(mime==null||!(extension.equals("jpg")&&mime.equals("image/jpeg")||extension.equals("png")&&mime.equals("image/png")||extension.equals("webp")&&mime.equals("image/webp")))
            throw new ValidationException("사진 형식을 확인하세요.");
        Path dir=root();Files.createDirectories(dir);String name=UUID.randomUUID()+"."+extension;Files.write(dir.resolve(name),bytes);return name;
    }
    private void remove(String name)throws IOException{if(name!=null&&!name.isBlank())Files.deleteIfExists(root().resolve(name));}
    private Path root(){return Path.of(AppConfig.required("ENJOYTRIP_UPLOAD_DIR")).toAbsolutePath().normalize();}
    private record Input(String title,String place,String type,LocalDate date,String description,Double lat,Double lng){}
}
