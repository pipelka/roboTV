package org.xvdr.extractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AdaptiveAllocator {

    final static String TAG = "AdaptiveAllocator";

    private List<SampleBuffer> m_list;
    private Comparator<SampleBuffer> mComparator = new Comparator<SampleBuffer>() {
        @Override
        public int compare(SampleBuffer lhs, SampleBuffer rhs) {
            return lhs.capacity() < rhs.capacity() ? -1 : 1;
        }
    };

    public AdaptiveAllocator(int initialBufferCount, int initalBufferSize) {
        m_list = new ArrayList<>(initialBufferCount);

        for(int i = 0; i < initialBufferCount; i++) {
            m_list.add(new SampleBuffer(initalBufferSize));
        }
    }

    synchronized public SampleBuffer allocate(int neededSize) {
        // check for a suitable packet
        SampleBuffer p = findAllocation(neededSize);

        if(p != null) {
            p.data().clear();
            return p;
        }

        // resize an unallocated packet
        p = findUnallocated();

        if(p != null) {
            p.resize(neededSize);
            p.data().clear();
            sort();
            return p;
        }

        // add a new packet
        p = new SampleBuffer(neededSize);
        m_list.add(p);

        p.allocate();
        sort();

        return p;
    }

    synchronized public void release(SampleBuffer a) {
        a.release();
    }

    private SampleBuffer findAllocation(int neededSize) {
        for(int i = 0; i < m_list.size(); i++) {
            SampleBuffer p = m_list.get(i);

            if(p.capacity() >= neededSize && p.allocate()) {
                return p;
            }
        }

        return null;
    }

    private SampleBuffer findUnallocated() {
        int i = m_list.size() - 1;
        while(i >= 0) {
            SampleBuffer p = m_list.get(i--);

            if(p.allocate()) {
                return p;
            }
        }

        return null;
    }

    private void sort() {
        Collections.sort(m_list, mComparator);
    }

    synchronized public void releaseAll() {
        for(int i = 0; i < m_list.size(); i++) {
            SampleBuffer a = m_list.get(i);
            a.release();
        }
    }

}
