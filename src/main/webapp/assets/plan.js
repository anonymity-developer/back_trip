function initPlan(context,planId,csrf){
  const query=document.getElementById('attraction-query'),id=document.getElementById('attraction-id'),results=document.getElementById('attraction-results');
  let timer;
  query.addEventListener('input',()=>{id.value='';clearTimeout(timer);results.replaceChildren();if(query.value.trim().length<2)return;
    timer=setTimeout(async()=>{try{const response=await fetch(context+'/api/attractions/search?q='+encodeURIComponent(query.value.trim()));if(!response.ok)return;
      for(const place of await response.json()){const button=document.createElement('button');button.type='button';button.textContent=place.title;button.addEventListener('click',()=>{id.value=place.id;query.value=place.title;results.replaceChildren();});results.append(button);}
    }catch(e){results.textContent='검색할 수 없습니다.';}},250);
  });
  document.getElementById('add-stop').addEventListener('submit',event=>{if(!id.value){event.preventDefault();query.setCustomValidity('검색 결과에서 여행지를 선택하세요.');query.reportValidity();}});
  query.addEventListener('input',()=>query.setCustomValidity(''));
  const list=document.getElementById('stop-list');let dragged=null;
  list.addEventListener('dragstart',event=>{dragged=event.target.closest('li[data-id]');});
  list.addEventListener('dragover',event=>{const item=event.target.closest('li[data-id]');if(!item||!dragged||item===dragged)return;event.preventDefault();const after=event.clientY>item.getBoundingClientRect().top+item.offsetHeight/2;list.insertBefore(dragged,after?item.nextSibling:item);});
  list.addEventListener('dragend',async()=>{if(!dragged)return;dragged=null;const body=new URLSearchParams({csrf});for(const item of list.querySelectorAll('li[data-id]'))body.append('stopId',item.dataset.id);
    try{const response=await fetch(context+'/api/plans/'+planId+'/stops/order',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body});if(!response.ok)throw Error();location.reload();}catch(e){alert('순서를 저장하지 못했습니다.');location.reload();}
  });
}
