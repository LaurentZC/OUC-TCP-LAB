package com.ouc.tcp.test.windows;

import com.ouc.tcp.test.elements.WindowElement;

/**
 * 滑动窗口抽象基类
 * 提供窗口管理的基本功能
 */
public abstract class SlidingWindow<T extends WindowElement> {
    protected final int size;          // 窗口大小
    protected final T[] window;        // 窗口数组
    protected int base;                // 窗口基序号

    /**
     * 构造函数
     *
     * @param size 窗口大小
     */
    protected SlidingWindow(int size) {
        this.size = size;
        this.window = createWindowArray(size);
        this.base = 0;

        // 模板方法：初始化窗口
        for (int i = 0; i < size; i++) {
            window[i] = createElement(i);
        }
    }

    protected abstract T[] createWindowArray(int size);

    protected abstract T createElement(int index);

    /**
     * 根据序号计算窗口索引
     *
     * @param seq 序号
     * @return 窗口索引
     */
    protected int getIdx(int seq) {
        return seq % size;
    }

    /**
     * 获取窗口大小
     *
     * @return 窗口大小
     */
    public int getSize() {
        return size;
    }

    /**
     * 获取窗口基序号
     *
     * @return 基序号
     */
    public int getBase() {
        return base;
    }
}