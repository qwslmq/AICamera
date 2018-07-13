package cn.whu.object_recognition;

public class Response {
    private long log_id;
    private int result_num;
    private Result[] result;

    class Result {
        private float score;
        private String root;
        private String keyword;

        public float getScore() {
            return score;
        }

        public String getKeyword() {
            return keyword;
        }

        public String getRoot() {
            return root;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public void setRoot(String root) {
            this.root = root;
        }

        public void setScore(float score) {
            this.score = score;
        }
    }

    public void setLog_id(long log_id) {
        this.log_id = log_id;
    }

    public void setResult_num(int result_num) {
        this.result_num = result_num;
    }

    public long getLog_id() {
        return log_id;
    }

    public int getResult_num() {
        return result_num;
    }

    public Result[] getResult() {
        return result;
    }

    public void setResult(Result[] result) {
        this.result = result;
    }
}
