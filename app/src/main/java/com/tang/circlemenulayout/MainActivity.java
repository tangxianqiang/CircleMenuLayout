package com.tang.circlemenulayout;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.Toast;

import com.tang.circlemenulayout.model.ChildBean;
import com.tang.circlemenulayout.widget.CircleMenuLayout;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CircleMenuLayout circleMenuLayout = (CircleMenuLayout) findViewById(R.id.circle_menu);
        String ppList[] = {"菜单A","菜单B"};//两个主菜单
        SparseArray<ArrayList<String>> pList = new SparseArray<>();
        ArrayList<String> pList1 = new ArrayList<>();//主菜单1对应的数据
        for (int i = 0; i < 7; i++) {
            pList1.add("item"+i+"of A");
        }
        ArrayList<String> pList2 = new ArrayList<>();//主菜单2对应的数据
        for (int i = 0; i < 5; i++) {
            pList2.add("item"+i+"of B");
        }
        pList.put(0,pList1);
        pList.put(1,pList2);
        circleMenuLayout.setData(ppList,pList,setData());
        circleMenuLayout.setOnCircleClickListener(new CircleMenuLayout.OnCircleClickListener() {
            @Override
            public void onCenterClick() {

            }

            @Override
            public void onItemInClick(View view) {

            }

            @Override
            public void onItemOutClick(ChildBean childBean) {
                Toast.makeText(MainActivity.this,childBean.getTitle()+"\n"+childBean.getContent(),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onItemOutLongClick(ChildBean childBean) {
                Toast.makeText(MainActivity.this,childBean.getTitle()+"\n"+childBean.getContent(),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDisMiss() {

            }
        });
    }

    /**
     * 设置最外层数据
     * @return
     */
    private ArrayList<ChildBean> setData(){
        ArrayList<ChildBean> children = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            ChildBean child = new ChildBean();
            if(i<25){
                child.setPpId(0);
                child.setTitle("菜单A");
            }else{
                child.setTitle("菜单B");
                child.setPpId(1);
            }
            if (i<=7) {
                child.setpId(0);
                child.setContent("item of 0");
            }else if(i<=13 && i>7){
                child.setpId(1);
                child.setContent("item of 1");
            }else if(i<=16 && i>13){
                child.setpId(2);
                child.setContent("item of 2");
            }else if(i<=18 && i>16){
                child.setpId(3);
                child.setContent("item of 3");
            }else if(i<=19 && i>18){
                child.setpId(4);
                child.setContent("item of 4");
            }else if(i<=21 && i>19){
                child.setpId(5);
                child.setContent("item of 5");
            }else if(i<=24 && i>21){
                child.setpId(6);
                child.setContent("item of 6");
            }else if(i<=34 && i>24){
                child.setpId(0);
                child.setContent("item of 0");
            }else if(i<=38 && i>34){
                child.setpId(1);
                child.setContent("item of 1");
            }else if(i<=42 && i>38){
                child.setpId(2);
                child.setContent("item of 2");
            }else if(i<=47 && i>42){
                child.setpId(3);
                child.setContent("item of 3");
            }else if(i<50 && i>47){
                child.setpId(4);
                child.setContent("item of 4");
            }
            children.add(child);
        }
        return children;
    }
}
