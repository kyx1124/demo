package demo1;

import sun.rmi.runtime.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class kiildemo {
    /**
    * 启动10用户线程
     * 库存
     * 生成一个合并队列，3个用户批一次
    * */
    public static void main(String[] args) throws InterruptedException {
        //构建一个线程池
        ExecutorService executorService = Executors.newCachedThreadPool();
        kiildemo kiildemo=new kiildemo();
        kiildemo.mergeJob();
        Thread.sleep(2000);
        List<Future<Result>> futuresList= new ArrayList<>();
        CountDownLatch countDownLatch=new CountDownLatch(10);
        for (int i=0;i<10;i++){
            final Long oderId=i+100l;
            final Long userId= Long.valueOf(i);
            Future<Result> future = executorService.submit(() ->{
                countDownLatch.countDown();
                return kiildemo.operate(new UserRequest(oderId,userId,1));

            });
            futuresList.add(future);
        }
        futuresList.forEach(future ->{
            try {
                Result result=future.get(300,TimeUnit.MILLISECONDS);
                System.out.println(Thread.currentThread().getName()+"客户端请求响应"+result);
            }catch (Exception e){
                e.printStackTrace();
            }
        });
    }
    //设置库存大小
    private Integer stock=6;
    //队列值设置
    private BlockingQueue<RequestPromise> queue=new LinkedBlockingQueue<>(10);

    private Result operate(UserRequest userRequest) throws InterruptedException {
        RequestPromise requestPromise=new RequestPromise(userRequest);
        //队列的创建
        boolean enqueue=queue.offer(requestPromise,100, TimeUnit.MILLISECONDS);
        if (! enqueue){
            return new Result(false,"系统繁忙");
        }
        synchronized (requestPromise) {
            try {
                requestPromise.wait(200);

            }catch (InterruptedException e){
                return new Result(false,"阻塞超时了");
            }
        }
        return requestPromise.getResult();

    }
    /**
     *定义一个定时任务
     *  先定义一个异步线程
     *
     **/
    public void mergeJob(){
        new Thread(() ->{
            while (true){
                List<RequestPromise> list=new ArrayList<>();
                if (queue.isEmpty()){
                    try {
                        Thread.sleep(10);
                        continue;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                while (queue.peek() !=null){
                    list.add(queue.poll());
                }
                System.out.println(Thread.currentThread().getName()+"合并扣减库存"+list);
                int sum = list.stream().mapToInt(e -> e.getUserRequest().getCount()).sum();
                //两种情况
                if (sum<=stock){
                    stock -=sum;
                    list.forEach(requestPromise -> {
                        requestPromise.setResult(new Result(true,"成功"));
                        synchronized (requestPromise){
                            requestPromise.notify();
                        }
                    });
                    continue;
                }
                for (RequestPromise requestPromise:list){
                    int count = requestPromise.getUserRequest().getCount();
                    if (count <= stock){
                        stock -=count;
                        requestPromise.setResult(new Result(true,"成功"));
                        synchronized (requestPromise){
                            requestPromise.notify();
                        }
                    }else {
                        requestPromise.setResult(new Result(false,"库存不够"));
                    }
                }





            }
        },"margeThread").start();
    }
//    private void rollback(UserRe){
//
//    }

}
class RequestPromise{

    private UserRequest userRequest;
    private Result result;

    public RequestPromise(UserRequest userRequest) {
        this.userRequest = userRequest;

    }

    public UserRequest getUserRequest() {
        return userRequest;
    }

    public void setUserRequest(UserRequest userRequest) {
        this.userRequest = userRequest;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "RequestPromise{" +
                "userRequest=" + userRequest +
                ", result=" + result +
                '}';
    }
}
class Result{
    private Boolean success;
    private String msg;
    public Result(boolean success,String msg){
        this.success=success;
        this.msg=msg;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }


    @Override
    public String toString() {
        return "Result{" +
                "success=" + success +
                ", msg='" + msg + '\'' +
                '}';
    }
}
class UserRequest{
    private Long orderId;
    private Long userId;
    private Integer count;

    UserRequest(Long orderId, Long userId, Integer count) {
        this.orderId = orderId;
        this.userId = userId;
        this.count = count;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "UserRequest{" +
                "orderId=" + orderId +
                ", userId=" + userId +
                ", count=" + count +
                '}';
    }
}
