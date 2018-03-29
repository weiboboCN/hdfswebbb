package WebHandler;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import Algorithm.EVENODD;
import Algorithm.LREVENODD;
import Algorithm.Replication;
import FSOpt.FileOperation;
import Model.AlgInfo;
import Model.FileInfo;
import Model.OptInfo;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DownloadFile extends HttpServlet {
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        doPost(req, res);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        String fileName = req.getParameter("fileName");
        String count = req.getParameter("count");
        ArrayList<FileInfo> fileList = (ArrayList<FileInfo>)req.getSession().getAttribute("fileList");
        ArrayList<OptInfo> optList = (ArrayList<OptInfo>)req.getSession().getAttribute("optList");
        System.out.println("before download fileList "+fileList.size());
        FileInfo targetFile = null;
        String resMessage = "";
        boolean flag = false;
        //search file in hdfs
        try{
            for(FileInfo item:fileList){
                System.out.println("itemfile "+item.getFileName()+" itemuser "+item.getUserName()+" fileName "+fileName+" usr "+count);
                if(item.getFileName().equals(fileName) && item.getUserName().equals(count)){
                    System.out.println("bingo!");
                    targetFile = item;
                    AlgInfo algInfo = item.getAlgInfo();
                    System.out.println("realAlginfo "+algInfo.getAlgName()+" "+algInfo.getParamK()+" "+algInfo.getParamN());
                    String algName = algInfo.getAlgName();
                    System.out.println("realAlgName??? "+algName);

                    Date date1 = new Date();
                    long time1=date1.getTime();
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String time = df.format(date1);
                    OptInfo optInfo = new OptInfo(fileName, count, "", "download", time, "", 0);
                    FileOperation fop = new FileOperation();

                    //PreDefination
                    Date date2 = null;
                    long time2= 0;
                    long resTime= 0;
                    String lastTime = null;

                    switch(algName){
                        case "replication":
                            Replication rpl = new Replication(algInfo.getParamK(), item, fop);
                            System.out.println("init blockNum "+rpl.blockNum);

                            //download to master
                            if(!rpl.decoding()){
                                System.out.println("decoding failed");
                                req.setAttribute("errorResult", "文件不存在,下载失败!");
                                RequestDispatcher dispatcher = req.getRequestDispatcher("FileInfo.jsp");
                                dispatcher.forward(req, res);
                            }

                            date2 = new Date();
                            time2=date2.getTime();
                            resTime=Math.abs(time2-time1);
                            lastTime = resTime+"ms";
                            optInfo.setOptLastTime(lastTime);
                            optInfo.setAlgName("replication");
                            optInfo.setOptUsedBytes(rpl.getOptFileSize());
                            optInfo.writeOneOptInfo();
                            optList.add(optInfo);
                            break;
                        case "EVENODD":
                            EVENODD evenodd = new EVENODD(algInfo.getParamK(), item, fop);
                            evenodd.RebuildFile("/home/luke/download/");

                            date2 = new Date();
                            time2=date2.getTime();
                            resTime=Math.abs(time2-time1);
                            lastTime = resTime+"ms";
                            optInfo.setOptLastTime(lastTime);
                            optInfo.setAlgName("EVENODD");
                            optInfo.setOptUsedBytes(evenodd.getOptFileSize());
                            optInfo.writeOneOptInfo();
                            optList.add(optInfo);
                            break;
                        case "LREVENODD":
                            System.out.println("in LREVENODD download");
                            LREVENODD lrevenodd = new LREVENODD(algInfo.getParamK(), item, fop);
                            lrevenodd.RebuildFile("/home/luke/download/");

                            date2 = new Date();
                            time2=date2.getTime();
                            resTime=Math.abs(time2-time1);
                            lastTime = resTime+"ms";
                            optInfo.setOptLastTime(lastTime);
                            optInfo.setAlgName("LREVENODD");
                            optInfo.setOptUsedBytes(lrevenodd.getOptFileSize());
                            System.out.println("write one opt info");
                            optInfo.writeOneOptInfo();
                            optList.add(optInfo);
                            break;
                    }
                    resMessage = "download success";
                    flag = true;
                    req.getSession().setAttribute("optList", optList);
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

        if(flag){
            //download to client
            //获取文件下载路径
            String path = "/home/luke/download/";
            System.out.println("real path? "+path + targetFile.getUserName()+"_"+fileName+targetFile.getFileType());
            File file = new File(path + targetFile.getUserName()+"_"+fileName+targetFile.getFileType());
            if(file.exists()){
                //设置相应类型让浏览器知道用什么打开  用application/octet-stream也可以，看是什么浏览器
                res.setContentType("application/x-msdownload");
                //设置头信息
                res.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
                InputStream inputStream = new FileInputStream(file);
                ServletOutputStream ouputStream = res.getOutputStream();
                byte b[] = new byte[1024];
                int n ;
                while((n = inputStream.read(b)) != -1){
                    ouputStream.write(b,0,n);
                }
                //关闭流
                ouputStream.close();
                inputStream.close();
            }else{
                System.out.println("file doesn't exist");
                resMessage = "download failed, file doesn't exist";
            }
        } else {
            resMessage = "download failed, target file doesn't exist";
        }
        req.getSession().setAttribute("message", resMessage);
        res.sendRedirect("FilePage.jsp");
    }
}