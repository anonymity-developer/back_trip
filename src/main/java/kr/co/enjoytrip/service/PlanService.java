package kr.co.enjoytrip.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import kr.co.enjoytrip.dao.AttractionDao;
import kr.co.enjoytrip.dao.PlanDao;
import kr.co.enjoytrip.dto.PlanDto;
import kr.co.enjoytrip.dto.PlanStopDto;

public class PlanService {
    private final PlanDao dao=new PlanDao();
    private final AttractionDao attractions=new AttractionDao();
    public List<PlanDto> list(Long viewer)throws SQLException{return dao.list(viewer);}
    public PlanDto visible(long id,Long viewer)throws SQLException{
        PlanDto plan=dao.find(id);
        if(plan==null||(!plan.isPublic()&&(viewer==null||plan.userId()!=viewer)))return null;
        return plan;
    }
    public PlanDto owned(long id,Long viewer)throws SQLException{
        PlanDto plan=dao.find(id);
        if(viewer==null||plan==null||plan.userId()!=viewer)throw new ValidationException("여행 계획을 수정할 권한이 없습니다.");
        return plan;
    }
    public List<PlanStopDto> stops(long id)throws SQLException{return dao.stops(id);}
    public long create(Long viewer,String title,String start,String end,String budget,String details,boolean isPublic)throws SQLException{
        if(viewer==null)throw new ValidationException("로그인이 필요합니다.");
        Input input=input(title,start,end,budget,details);
        return dao.insert(viewer,input.title,input.start,input.end,input.budget,input.details,isPublic);
    }
    public void update(long id,Long viewer,String title,String start,String end,String budget,String details,boolean isPublic)throws SQLException{
        owned(id,viewer);Input input=input(title,start,end,budget,details);
        for(PlanStopDto stop:dao.stops(id))
            if(stop.visitDate().isBefore(input.start)||stop.visitDate().isAfter(input.end))
                throw new ValidationException("기존 방문일이 새 여행 기간 밖에 있습니다.");
        if(!dao.update(id,viewer,input.title,input.start,input.end,input.budget,input.details,isPublic))throw new ValidationException("수정할 수 없습니다.");
    }
    public void delete(long id,Long viewer)throws SQLException{owned(id,viewer);dao.delete(id,viewer);}
    public void addStop(long id,Long viewer,long attractionId,String date,String budget,String memo)throws SQLException{
        PlanDto plan=owned(id,viewer);LocalDate day=parseDate(date);
        if(day.isBefore(plan.startDate())||day.isAfter(plan.endDate()))throw new ValidationException("여행 기간 안의 날짜를 선택하세요.");
        if(attractions.find(attractionId)==null)throw new ValidationException("여행지를 찾을 수 없습니다.");
        if(memo!=null&&memo.length()>1000)throw new ValidationException("메모는 1000자 이하로 입력하세요.");
        dao.addStop(id,attractionId,day,money(budget),memo);
    }
    public void deleteStop(long id,Long viewer,long stopId)throws SQLException{owned(id,viewer);if(!dao.deleteStop(id,stopId))throw new ValidationException("방문지를 찾을 수 없습니다.");}
    public void reorder(long id,Long viewer,String[] values)throws SQLException{
        owned(id,viewer);if(values==null||values.length>500)throw new ValidationException("잘못된 순서입니다.");
        long[] ids=new long[values.length];try{for(int i=0;i<values.length;i++)ids[i]=Long.parseLong(values[i]);}catch(NumberFormatException e){throw new ValidationException("잘못된 순서입니다.");}
        dao.reorder(id,ids);
    }
    private Input input(String title,String start,String end,String budget,String details){
        if(title==null||title.isBlank()||title.length()>200||details!=null&&details.length()>20000)throw new ValidationException("제목 또는 상세 설명 길이를 확인하세요.");
        LocalDate from=parseDate(start),to=parseDate(end);
        if(to.isBefore(from)||to.isAfter(from.plusDays(90)))throw new ValidationException("여행 기간은 1~91일로 입력하세요.");
        return new Input(title.trim(),from,to,money(budget),details==null?"":details.trim());
    }
    private LocalDate parseDate(String value){try{return LocalDate.parse(value);}catch(DateTimeParseException|NullPointerException e){throw new ValidationException("날짜 형식을 확인하세요.");}}
    private BigDecimal money(String value){try{BigDecimal n=new BigDecimal(value==null||value.isBlank()?"0":value);if(n.signum()<0||n.compareTo(new BigDecimal("9999999999.99"))>0)throw new NumberFormatException();return n;}catch(NumberFormatException e){throw new ValidationException("경비는 0 이상의 금액으로 입력하세요.");}}
    private record Input(String title,LocalDate start,LocalDate end,BigDecimal budget,String details){}
}
