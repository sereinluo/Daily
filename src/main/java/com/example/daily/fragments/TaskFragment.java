package com.example.daily.fragments;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.daily.R;
import com.example.daily.tasks.TaskItem;

import java.util.ArrayList;
import java.util.List;

public class TaskFragment extends Fragment {
    private static final String TAG = TaskFragment.class.getName();

    private List<TaskItem> taskList = new ArrayList<>();
    private TaskAdapter taskAdapter;

    private final List<TextView> typeMenuList = new ArrayList<>();

    private boolean isHideCompleted = false;
    private String TypeNow = "全部";
    private View view;
    private MySQLiteOpenHelper openHelper;
    private SQLiteDatabase writeDatabase;
    private SQLiteDatabase readDatabase;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.task, null);

        //数据库初始化
        openHelper = new MySQLiteOpenHelper(getActivity());
        writeDatabase = openHelper.getWritableDatabase();
        readDatabase = openHelper.getReadableDatabase();

        // 获取数据库数据模块
        ListView taskListView = view.findViewById(R.id.taskListView);
        taskAdapter = new TaskAdapter();
        taskListView.setAdapter(taskAdapter);
        ReadTaskFromDatabase();

        //设置菜单点击事件
        SetTypeMenuOnClick(view);

        //隐藏已完成Switch
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch hideCompletedTaskSwitch = view.findViewById(R.id.HideCompletedTaskView);
        hideCompletedTaskSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(isChecked)   isHideCompleted = true;
                else            isHideCompleted = false;
                ReadTaskFromDatabase();
            }
        });

        //设置添加事项的图片按钮
        SetAddTaskImageOnClick();

        //-- onCreateView End --//
        return view;
    }


    /*
    0  id       : id号
    1  type     : 事项类型（总共四种：默认，工作，学习，生活）
    2  level    : 事项优先级 0~3 四个优先级
    3  content  : 事项内容
    4  info     : 事项备注
    5  status   : 事项状态（ 0 未完成、1 完成、-1 失败）
     */



    /** 菜单栏模块 **/
    public void SetTypeMenuOnClick(View view){
        typeMenuList.add((TextView) view.findViewById(R.id.TypeMenu_default));
        typeMenuList.add((TextView) view.findViewById(R.id.TypeMenu_work));
        typeMenuList.add((TextView) view.findViewById(R.id.TypeMenu_study));
        typeMenuList.add((TextView) view.findViewById(R.id.TypeMenu_life));

        int[] color = {
                getResources().getColor(R.color.defaultColor, null),
                getResources().getColor(R.color.workColor, null),
                getResources().getColor(R.color.studyColor, null),
                getResources().getColor(R.color.lifeColor, null),
        };

        for(int i=0; i<4 ;i++){
            int finalI = i; //分类索引值
            typeMenuList.get(i).setOnClickListener(v -> {
                // 点击分类的一项后设置样式
                typeMenuList.get(finalI).setTextColor(Color.BLACK);
                typeMenuList.get(finalI).setBackgroundColor(Color.WHITE);

                typeMenuList.get((finalI+1) % 4).setBackgroundColor(color[(finalI+1) % 4]);
                typeMenuList.get((finalI+1) % 4).setTextColor(Color.WHITE);

                typeMenuList.get((finalI+2) % 4).setBackgroundColor(color[(finalI+2) % 4]);
                typeMenuList.get((finalI+2) % 4).setTextColor(Color.WHITE);

                typeMenuList.get((finalI+3) % 4).setBackgroundColor(color[(finalI+3) % 4]);
                typeMenuList.get((finalI+3) % 4).setTextColor(Color.WHITE);

                // 显示某一类待办数据，这里筛选taskList即可
                List<TaskItem> typeTaskList = new ArrayList<>();
                String[] types = {"全部", "工作","学习","生活"};
                /*  分类索引值
                0 全部
                1 工作
                2 学习
                3 生活
                 */
                // 点击工作 学习 生活时分类
                TypeNow = types[finalI];
                Log.i(TAG, "SetTypeMenuOnClick: "+TypeNow);
                ReadTaskFromDatabase();

            });

        }

    }


    /** 待办事项数据管理模块 **/


    public void InsertTaskToDatabase(TaskItem task){
        Log.i(TAG, "InsertTaskToDatabase: "+task.getContent());
        MySQLiteOpenHelper mySQLiteOpenHelper = new MySQLiteOpenHelper(getActivity());
        SQLiteDatabase writeDateBase = mySQLiteOpenHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("type", task.getType());
        values.put("level", task.getLevel());
        values.put("content", task.getContent());
        values.put("info", task.getInfo());
        values.put("status", task.getStatus());

        writeDateBase.insert("task", null, values);
        writeDateBase.close();

        ReadTaskFromDatabase();
    }
    public void ReadTaskFromDatabase(){
        if (taskList.size()!=0) {
            taskList.clear();
        }

        @SuppressLint("Recycle") Cursor cursor = readDatabase.query(
                "task",
                new String[]{"id", "type", "level","content", "info", "status"},
                null,
                null,
                null,
                null,
                null
        );


        //隐藏，有分类
        if(isHideCompleted && !TypeNow.equals("全部")){
            //只获取未完成事项
            while(cursor.moveToNext()){
                if((cursor.getInt(5) == 0 ) && (cursor.getString(1).equals(TypeNow))){
                    TaskItem task = new TaskItem(
                            cursor.getInt(0),
                            cursor.getString(1),
                            cursor.getInt(2),
                            cursor.getString(3),
                            cursor.getString(4),
                            cursor.getInt(5)
                    );
                    taskList.add(task);
                }

            }

        }
        //不隐藏，有分类
        if(!isHideCompleted && !TypeNow.equals("全部")){
            while(cursor.moveToNext()){
                if(cursor.getString(1).equals(TypeNow)){
                    TaskItem task = new TaskItem(
                            cursor.getInt(0),
                            cursor.getString(1),
                            cursor.getInt(2),
                            cursor.getString(3),
                            cursor.getString(4),
                            cursor.getInt(5)
                    );
                    taskList.add(task);
                }
            }
        }
        //隐藏，不分类
        if(isHideCompleted && TypeNow.equals("全部")){
            while(cursor.moveToNext()){
                if(cursor.getInt(5) == 0){
                    TaskItem task = new TaskItem(
                            cursor.getInt(0),
                            cursor.getString(1),
                            cursor.getInt(2),
                            cursor.getString(3),
                            cursor.getString(4),
                            cursor.getInt(5)
                    );
                    taskList.add(task);
                }

            }
        }
        else{
            while(cursor.moveToNext()){
                TaskItem task = new TaskItem(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getInt(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getInt(5)
                );
                taskList.add(task);
            }
        }


        Log.i(TAG, "ReadTaskDataFromSQL: taskList:");
        Log.i(TAG, "ReadTaskDataFromSQL: id     type    level   status  content         info");
        for(TaskItem task : taskList){
            Log.i(TAG, "ReadTaskDataFromSQL: "
                    +task.getId()+"      "
                    +task.getType()+"     "
                    +task.getLevel()+"       "
                    +task.getStatus()+"       "
                    +task.getContent()+"    "
                    +task.getInfo()
            );
        }
        taskAdapter.notifyDataSetChanged();

    }
    public void UpDateTaskToDatabase(TaskItem update_task){

        ContentValues update_values = new ContentValues();
        update_values.put("type", update_task.getType());
        update_values.put("level", update_task.getLevel());
        update_values.put("status", update_task.getStatus());
        update_values.put("content", update_task.getContent());
        update_values.put("info", update_task.getInfo());

        int update_res = writeDatabase.update(
                "task",
                update_values,
                "id=?",
                new String[]{""+update_task.getId()}
        );

        Log.i(TAG, "UpDateTaskToDatabase: update num = "+update_res);

        ReadTaskFromDatabase();
    }
    public void DeleteTaskToDatabase(TaskItem delete_task){
        int delete_id = delete_task.getId();
        writeDatabase.delete("task", "id=?", new String[]{""+delete_id});

        ReadTaskFromDatabase();
    }

    public static class MySQLiteOpenHelper extends SQLiteOpenHelper{

        public MySQLiteOpenHelper(@Nullable Context context) {
            super(context, "Daily.db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.i(TAG, "onCreate: sqlite");
            //创建待办事项数据表
            String create_sql =
                    "create table task(" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "type varchar(50),"+
                            "level int, " +
                            "content varchar(50), " +
                            "info varchar(200),"+
                            "status int);";
            db.execSQL(create_sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    public class TaskAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            return taskList.size();
        }

        @Override
        public Object getItem(int position) {
            return taskList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return taskList.get(position).getId();
        }

        @SuppressLint("InflateParams")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.i(TAG, "getView: "+position);
            ViewHolder viewHolder;
            TaskItem task = (TaskItem) getItem(position);
            if(convertView == null){
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.task_item, null);
                
                viewHolder.taskImage = convertView.findViewById(R.id.taskImageView);
                viewHolder.taskContent = convertView.findViewById(R.id.taskContentView);
                viewHolder.taskInfo = convertView.findViewById(R.id.taskInfoView);
                viewHolder.taskLevel = convertView.findViewById(R.id.taskLevelView);
                
                convertView.setTag(viewHolder);
            }else{
                viewHolder = (ViewHolder) convertView.getTag();
            }


            //设置图标及监听事件
            ShowTaskImage(convertView, task);
            ShowTaskImageOnClick(convertView, task);


            // 设置事项重要级别 level :  0~3 四个优先级
            ShowTaskLevel(convertView, task.getLevel());

            // 设置事项内容 根据status设置样式
            ShowTaskContent(convertView, task);

            //设置每条事项的长按事件：弹出对话框显示事项的内容
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ShowTaskInfoDialog(task);
                }
            });

            viewHolder.taskInfo.setText(task.getInfo());

            return convertView;
        }
        
    }
    public static class ViewHolder{
        ImageView taskImage;
        TextView taskLevel;
        TextView taskContent;
        TextView taskInfo;
    }

    public void ShowTaskImage(View convertView,TaskItem task){
        Log.i(TAG, "ShowTaskImage: "+task.getType()+" "+task.getStatus());
        // 此处传入 缓存convertView 参数, 便于操作
        ImageView image = ((ViewHolder) convertView.getTag()).taskImage;
        String type = task.getType();
        int status = task.getStatus();
        
        if(type.equals("工作")){
            if(status == 0)     image.setImageResource(R.drawable.task_work);
            if(status == -1)    image.setImageResource(R.drawable.fail_work);
            if(status == 1)     image.setImageResource(R.drawable.complete_work);
        }
        if(type.equals("学习")){
            if(status == 0)     image.setImageResource(R.drawable.task_study);
            if(status == -1)    image.setImageResource(R.drawable.fail_study);
            if(status == 1)     image.setImageResource(R.drawable.complete_study);
        }
        if(type.equals("生活")){
            if(status == 0)     image.setImageResource(R.drawable.task_life);
            if(status == -1)    image.setImageResource(R.drawable.fail_life);
            if(status == 1)     image.setImageResource(R.drawable.complete_life);

        }
        if(type.equals("全部")){
            if(status == 0)     image.setImageResource(R.drawable.task_default);
            if(status == -1)    image.setImageResource(R.drawable.fail_default);
            if(status == 1)     image.setImageResource(R.drawable.complete_default);
        }

    }

    public void ShowTaskImageOnClick(View convertView,TaskItem task){
        ImageView image = ((ViewHolder) convertView.getTag()).taskImage;
        TextView content = ((ViewHolder) convertView.getTag()).taskContent;

        String type = task.getType();
        int status = task.getStatus();

        TaskItem update_task = new TaskItem(
                task.getId(),
                task.getType(),
                task.getLevel(),
                task.getContent(),
                task.getInfo(),
                (task.getStatus()+1) % 2
        );

        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(type.equals("工作")){
                    if (status == 0){
                        image.setImageResource(R.drawable.complete_work);
                        content.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
                        content.setTextColor(getResources().getColor(R.color.GRAY, null));

                    }
                    if (status == 1){
                        image.setImageResource(R.drawable.task_work);
                        content.getPaint().setFlags(0);
                        content.setTextColor(getResources().getColor(R.color.black, null));
                    }
                }
                if(type.equals("学习")){
                    if (status == 0){
                        image.setImageResource(R.drawable.complete_study);
                        content.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
                        content.setTextColor(getResources().getColor(R.color.GRAY, null));
                    }
                    if (status == 1){
                        image.setImageResource(R.drawable.task_study);
                        content.getPaint().setFlags(0);
                        content.setTextColor(getResources().getColor(R.color.black, null));
                    }
                }
                if(type.equals("生活")){
                    if (status == 0){
                        image.setImageResource(R.drawable.complete_life);
                        content.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
                        content.setTextColor(getResources().getColor(R.color.GRAY, null));
                    }
                    if (status == 1){
                        image.setImageResource(R.drawable.task_life);
                        content.getPaint().setFlags(0);
                        content.setTextColor(getResources().getColor(R.color.black, null));
                    }
                }
                if(type.equals("默认")){
                    if (status == 0){
                        image.setImageResource(R.drawable.complete_default);
                        content.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
                        content.setTextColor(getResources().getColor(R.color.GRAY, null));
                    }
                    if (status == 1){
                        image.setImageResource(R.drawable.task_default);
                        content.getPaint().setFlags(0);
                        content.setTextColor(getResources().getColor(R.color.black, null));
                    }
                }

                UpDateTaskToDatabase(update_task);

            }
        });
    }

    public void ShowTaskLevel(View convertView, int level){
        // 设置事项重要级别 level :  0~3 四个优先级 Ⅰ Ⅱ Ⅲ Ⅳ
        TextView levelText = ((ViewHolder) convertView.getTag()).taskLevel;


        if(level == 0){
            levelText.setText("Ⅰ");
            levelText.setTextColor(getResources().getColor(R.color.level_0, null));
        }
        if(level == 1){
            levelText.setText("Ⅱ");
            levelText.setTextColor(getResources().getColor(R.color.level_1, null));
        }
        if(level == 2){
            levelText.setText("Ⅲ");
            levelText.setTextColor(getResources().getColor(R.color.level_2, null));
        }
        if(level == 3){
            levelText.setText("Ⅳ");
            levelText.setTextColor(getResources().getColor(R.color.level_3, null));
        }

    }

    public void ShowTaskContent(View convertView, TaskItem task){
        TextView content = ((ViewHolder) convertView.getTag()).taskContent;
        int status = task.getStatus();

        content.setText(task.getContent());

        //事项已完成 中划线 灰色
        if(status == 1){
            content.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
            content.setTextColor(getResources().getColor(R.color.GRAY, null));
        }
        //事项未完成 无中划线 黑色
        if(status == 0){
            content.getPaint().setFlags(0);
            content.setTextColor(getResources().getColor(R.color.black, null));
        }
        //事项失败 无中划线 灰色
        if(status == -1){
            content.getPaint().setFlags(0);
            content.setTextColor(getResources().getColor(R.color.GRAY, null));
        }

    }

    public void SetAddTaskImageOnClick(){
        //添加事项的按钮
        ImageView addTaskImage = (ImageView) view.findViewById(R.id.addTaskImage);
        addTaskImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShowAddTaskDialog();
            }
        });
    }

    public void ShowAddTaskDialog(){
        View addView = getLayoutInflater().inflate(R.layout.add_task_dialog, null);

        final AlertDialog addDialog = new AlertDialog.Builder(getActivity()).setView(addView).create();
        addDialog.show();

        //获取对话框中的布局控件
        Button cancelButton = (Button) addView.findViewById(R.id.cancelAddButton);
        Button confirmButton = (Button) addView.findViewById(R.id.confirmAddButton);
        EditText contentEdit = (EditText) addView.findViewById(R.id.addTaskContentEdit);
        EditText infoEdit = (EditText) addView.findViewById(R.id.addTaskInfoEdit);
        RadioGroup typeGroup = (RadioGroup) addView.findViewById(R.id.typeRadioGroup);
        RadioGroup levelGroup = (RadioGroup) addView.findViewById(R.id.levelRadioGroup);

        typeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

            }
        });
        levelGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

            }
        });

        //确定按钮
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取输入的事项内容和备注
                String addContent = contentEdit.getText().toString();
                String addInfo = infoEdit.getText().toString();

                //RadioGroup的选择项
                RadioButton typeSelectBtn = (RadioButton) addView.findViewById(typeGroup.getCheckedRadioButtonId());
                String addType = typeSelectBtn.getText().toString();
                RadioButton levelSelectBtn = (RadioButton) addView.findViewById(levelGroup.getCheckedRadioButtonId());
                int addLevel = Integer.parseInt(levelSelectBtn.getText().toString().substring(0,1));

                InsertTaskToDatabase(
                        new TaskItem(addType, addLevel, addContent, addInfo, 0)
                );
                addDialog.dismiss();
            }
        });

        // 取消按钮
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addDialog.dismiss();
            }
        });



    }

    public void ShowTaskInfoDialog(TaskItem task){
        // 获取传入的事项数据
        String content = task.getContent();
        String type = task.getType();
        int level = task.getLevel();
        String info = task.getInfo();

        //获取布局
        View infoView = getLayoutInflater().inflate(R.layout.task_info_dialog, null);

        final AlertDialog infoDialog = new AlertDialog.Builder(getActivity()).setView(infoView).create();
        infoDialog.show();

        //获取对话框中的布局控件
        EditText contentEdit = (EditText) infoView.findViewById(R.id.addTaskContentEdit);
        EditText infoEdit = (EditText) infoView.findViewById(R.id.addTaskInfoEdit);
        RadioGroup typeGroup = (RadioGroup) infoView.findViewById(R.id.typeRadioGroup);
        RadioGroup levelGroup = (RadioGroup) infoView.findViewById(R.id.levelRadioGroup);
        ImageView deleteImage = (ImageView) infoView.findViewById(R.id.deleteTaskButton);
        ImageView modifyImage = (ImageView) infoView.findViewById(R.id.modifyTaskButton);
        ImageView failImage = (ImageView) infoView.findViewById(R.id.failTaskButton);


        //显示task事项信息
        contentEdit.setText(content);
        infoEdit.setText(info);
        SetTypeRadioGroupSelected(typeGroup, type);
        SetLevelRadioGroupSelected(levelGroup, level);

        //删除按钮
        deleteImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeleteTaskToDatabase(task);
                infoDialog.dismiss();
            }
        });

        //失败按钮
        failImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                task.setStatus(-1);
                UpDateTaskToDatabase(task);
                //别忘记关闭对话框
                infoDialog.dismiss();
            }
        });


        //修改按钮
        modifyImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取输入的事项内容和备注
                String modifyContent = contentEdit.getText().toString();
                String modifyInfo = infoEdit.getText().toString();

                //RadioGroup的选择项
                RadioButton typeSelectBtn = (RadioButton) infoView.findViewById(typeGroup.getCheckedRadioButtonId());
                String modifyType = typeSelectBtn.getText().toString();
                RadioButton levelSelectBtn = (RadioButton) infoView.findViewById(levelGroup.getCheckedRadioButtonId());
                int modifyLevel = Integer.parseInt(levelSelectBtn.getText().toString().substring(0,1));

                task.setContent(modifyContent);
                task.setInfo(modifyInfo);
                task.setType(modifyType);
                task.setLevel(modifyLevel);

                UpDateTaskToDatabase(task);
                //别忘记关闭对话框
                infoDialog.dismiss();
            }
        });

    }
    public void SetTypeRadioGroupSelected(RadioGroup group, String type){
        switch (type){
            case "工作":
                ((RadioButton) group.findViewById(R.id.workButton)).setChecked(true);
                break;
            case "学习":
                ((RadioButton) group.findViewById(R.id.studyButton)).setChecked(true);
                break;
            case "生活":
                ((RadioButton) group.findViewById(R.id.lifeButton)).setChecked(true);
                break;
            case "全部":
                ((RadioButton) group.findViewById(R.id.defaultButton)).setChecked(true);
                break;
        }
    }
    public void SetLevelRadioGroupSelected(RadioGroup group, int level){
        switch (level){
            case 0:
                ((RadioButton) group.findViewById(R.id.level0Button)).setChecked(true);
                break;
            case 1:
                ((RadioButton) group.findViewById(R.id.level1Button)).setChecked(true);
                break;
            case 2:
                ((RadioButton) group.findViewById(R.id.level2Button)).setChecked(true);
                break;
            case 3:
                ((RadioButton) group.findViewById(R.id.level3Button)).setChecked(true);
                break;
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(openHelper != null){
            openHelper.close();
        }
        if(readDatabase != null){
            readDatabase.close();
        }
        if(writeDatabase != null){
            writeDatabase.close();
        }

    }
}
