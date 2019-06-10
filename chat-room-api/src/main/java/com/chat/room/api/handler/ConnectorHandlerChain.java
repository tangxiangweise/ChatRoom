package com.chat.room.api.handler;

public abstract class ConnectorHandlerChain<Model> {

    private volatile ConnectorHandlerChain<Model> next;

    public ConnectorHandlerChain<Model> appendLast(ConnectorHandlerChain<Model> newChain) {
        if (newChain == this || this.getClass().equals(newChain.getClass())) {
            return this;
        }
        synchronized (this) {
            if (next == null) {
                next = newChain;
                return newChain;
            }
            return next.appendLast(newChain);
        }
    }

    public synchronized boolean remove(Class<? extends ConnectorHandlerChain<Model>> clx) {
        // 自己不能移除自己，因为自己未持有上一个链接的引用
        if (this.getClass().equals(clx)) {
            return false;
        }
        synchronized (this) {
            if (next == null) {
                // 当前无下一个节点存在，无法判断
                return false;
            } else if (next.getClass().equals(clx)) {
                // 移除next节点
                next = next.next;
                return true;
            } else {
                // 交给next进行移除操作
                return next.remove(clx);
            }
        }
    }

    synchronized boolean handle(ClientHandler handler, Model model) {
        ConnectorHandlerChain<Model> next = this.next;
        if (consume(handler, model)) {
            return true;
        }
        boolean consumed = next != null && next.handle(handler, model);
        if (consumed) {
            return true;
        }
        return consumeAgain(handler, model);
    }

    protected abstract boolean consume(ClientHandler handler, Model model);

    protected boolean consumeAgain(ClientHandler handler, Model model) {
        return false;
    }
}